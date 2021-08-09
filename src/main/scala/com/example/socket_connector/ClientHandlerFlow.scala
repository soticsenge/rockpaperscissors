package com.example.socket_connector

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.ActorSink
import com.example.socket_connector.messages.WebsocketMsg

import java.util.UUID

class ClientHandlerFlow()(implicit val system: ActorSystem[_]) {
  def websocketFlow(): Flow[Message, Message, NotUsed] = {
    val handler = system.systemActorOf(ClientHandlerActor(), "handler_" + UUID.randomUUID())
    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message]
        .collect {
          case TextMessage.Strict(text) =>
            system.log.info(s"Transforming incoming msg [$text] into domain msg.")
            WebsocketMsg.Incoming(text)
        }
        .to(ActorSink.actorRef[WebsocketMsg.Incoming](handler ,WebsocketMsg.Incoming(""), _ => WebsocketMsg.Incoming("")))

    val outgoingMessages: Source[Message, NotUsed] =
      Source.actorRef[WebsocketMsg](10, OverflowStrategy.fail)
        .mapMaterializedValue { userActor =>
          handler ! WebsocketMsg.Connected(userActor)
          NotUsed
        }
        .collect {
          case outMsg: WebsocketMsg.Outgoing =>
            system.log.info(s"Transforming domain msg [$outMsg] to websocket msg.")
            TextMessage(outMsg.text)
        }
    Flow.fromSinkAndSourceCoupled(incomingMessages, outgoingMessages)
  }
}
