package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, Cancellable}
import akka.stream.ActorMaterializer
import akka.stream.OverflowStrategy._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by alex on 01.11.16.
  */
@Singleton
class StreamController @Inject()()(implicit exec: ExecutionContext, actorSystem: ActorSystem) extends Controller {
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
}
