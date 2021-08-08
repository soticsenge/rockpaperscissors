package com.example

import akka.NotUsed
import akka.actor.{ExtendedActorSystem, Terminated}
import akka.actor.TypedActor.context
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.OverflowStrategy
import akka.actor.typed.scaladsl.adapter._
import akka.stream.scaladsl.{Flow, Sink, Source}

import java.util.UUID

class ClientHandlerFlow(implicit val system: ActorSystem[_]) {
  def websocketFlow(): Flow[Message, Message, NotUsed] = {
    val handler = system.toClassic.asInstanceOf[ExtendedActorSystem].systemActorOf(ClientHandlerActor.props, "handler" + UUID.randomUUID())
    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message]
        .collect {
          case TextMessage.Strict(text) =>
            println(
              s"Transforming incoming msg [$text] into domain msg"
            )
            WebsocketMsg.Incoming(text)
        }
        .to(Sink.actorRef[WebsocketMsg.Incoming](handler, Terminated))
    val outgoingMessages: Source[Message, NotUsed] =
      Source
        .actorRef[WebsocketMsg.Outgoing](10, OverflowStrategy.fail)
        .mapMaterializedValue { userActor =>
          handler ! WebsocketMsg.Connected(userActor)
          NotUsed
        }
        .collect {
          case outMsg: WebsocketMsg.Outgoing =>
            println(
              s"Transforming domain msg [$outMsg] to websocket msg"
            )
            TextMessage(outMsg.text)
        }
    Flow.fromSinkAndSourceCoupled(incomingMessages, outgoingMessages)
  }
}
