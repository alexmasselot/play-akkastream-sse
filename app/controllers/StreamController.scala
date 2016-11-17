package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, Props}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.OverflowStrategy._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, SourceQueue}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._

/**
  * Created by alex on 01.11.16.
  */
@Singleton
class StreamController @Inject()()(implicit context: ExecutionContext, actorSystem: ActorSystem) extends Controller {
  implicit val materializer = ActorMaterializer()

  /**
    * a text 10 times a seconds, exposed by tSource.tick
    * works fine
    *
    * @return
    */
  def tick = Action {
    val tickSource = Source.tick(0 millis, 100 millis, "TICK")
    Ok.chunked(tickSource via EventSource.flow).as(ContentTypes.EVENT_STREAM)
  }

  def actorFlow = Action {
    val (ref, publisher) = Source.actorRef[String](10, fail).toMat(Sink.asPublisher(true))(Keep.both).run()

    ref ! "PAF"
    actorSystem.scheduler.scheduleOnce(1000 milliseconds, ref, "delayed PIF")


    Ok.chunked(Source.fromPublisher(publisher) via EventSource.flow).as(ContentTypes.EVENT_STREAM)
  }

  def actorFlowWorking = Action {
    val (ref, publisher) = Source.actorRef[String](10, fail).toMat(Sink.asPublisher(true))(Keep.both).run()

    ref ! "PAF"
    actorSystem.scheduler.scheduleOnce(1000 milliseconds, ref, "delayed PIF")

    Ok.chunked(Source.fromPublisher(publisher) via EventSource.flow).as(ContentTypes.EVENT_STREAM)
  }

  /*
  Every second, a message is sent to and ActorForward, which just forwards the message to the actor tied to the akka stream source.
  The goal is to catch the SSE interruption and send a Stop message to this actor, which shall then exit

  Thanks for the code http://loicdescotte.github.io/posts/play-akka-streams-queue/
   */
  def actorFlowInterrupt = Action {
    case object Stop {}
    class ActorForward(queue: SourceQueue[String]) extends Actor with ActorLogging {
      override def receive: Receive = {
        case Stop =>
          println("actor forward exits")
          context stop self
        case x: String =>
          println(s"piping message $x")
          queue.offer(x)
      }
    }

    val Tick = "tick"

    class TickActor(queue: SourceQueue[String]) extends Actor {
      def receive = {
        case Tick => queue.offer("tack")
      }
    }
    def peekMatValue[T, M](src: Source[T, M]): (Source[T, M], Future[M]) = {
      val p = Promise[M]
      val s = src.mapMaterializedValue { m =>
        p.trySuccess(m)
        m
      }
      (s, p.future)
    }
    val (queueSource, futureQueue) = peekMatValue(Source.queue[String](10, OverflowStrategy.fail))

    futureQueue.map { queue =>

      val actorForward = actorSystem.actorOf(Props(new ActorForward(queue)))

      actorForward ! "PAF"
      val scheduledPif = actorSystem.scheduler.schedule(0 milliseconds, 1000 milliseconds, actorForward, "PIF")

      queue.watchCompletion().map { done =>
        println("Client disconnected")
        scheduledPif.cancel
        actorForward ! Stop
        println("Scheduler canceled")
      }
    }

    Ok.chunked(
      queueSource.map { e =>
        println("queue source element : " + e)
        e
      }
        via EventSource.flow
    )


  }
}
