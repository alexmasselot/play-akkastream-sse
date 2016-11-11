package controllers

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.OverflowStrategy._
import akka.stream.scaladsl.{Flow, Sink, Source}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json.{JsValue, Json}
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
    val source = Source.actorRef[String](10, fail)
      .map({ (x) =>
        //the message go through this stage
        println(s"through source '$x'")
        x
      })

    val ref = Flow[String]
      .to(Sink.ignore)
      .runWith(
        source
      )

    //send messages. One is now, oth is delayed
    ref ! "PAF"
    actorSystem.scheduler.scheduleOnce(1000 milliseconds, ref, "delayed PIF")

    Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM)
  }
}
