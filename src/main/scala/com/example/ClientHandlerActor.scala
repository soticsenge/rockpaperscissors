package com.example

import akka.actor.{Actor, PoisonPill, Props, Terminated, ActorRef => ClassiCActorRef}
import com.example.Domain.{CreateGameMessageFromConnectorActor, CreateGameResponseToClient, PlayerMovementMessageFromConnectorActor}
import io.circe.HCursor
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.parser._
import io.circe.syntax._

object ClientHandlerActor {
  def props: Props = Props(new ClientHandlerActor)
}

class ClientHandlerActor extends Actor {
  override def receive: Receive = onMessage(Set.empty)

  private def onMessage(userActors: Set[ClassiCActorRef]): Receive = {

    case WebsocketMsg.Connected(userActor) =>
      context.watch(userActor)
      context.become(onMessage(userActors + userActor))

    case Terminated(userActor) =>
      val openUserActors: Set[ClassiCActorRef] = userActors - userActor
      if (openUserActors.isEmpty) {
        self ! PoisonPill
      } else {
        context.become(onMessage(openUserActors))
      }

    case WebsocketMsg.Incoming(text) =>
      parse(StringContext.processEscapes(text.slice(1, text.length - 1))) match {
        case Left(_) =>
          self ! Domain.Error(s"Cannot parse $text as JSON")
        case Right(value) =>
          val cursor: HCursor = value.hcursor
          cursor.get[String]("messageType") match {
            case Right("createGameRequest") => {
              value.as[Domain.CreateGameFromClient] match {
                case Left(_) =>
                  self ! Domain.Error(s"Cannot parse $value as Domain")
                case Right(event) =>
                  self ! event
              }
            }
            case Right("gameMoveRequest") => {
              value.as[Domain.PlayerMovementMessageFromClient] match {
                case Left(_) =>
                  self ! Domain.Error(s"Cannot parse $value as Domain")
                case Right(event) =>
                  self ! event
              }
            }
            case _ => self ! Domain.Error(s"Cannot parse $text as Domain - invalid message")
          }
      }

    case outgoing: WebsocketMsg.Outgoing => userActors.foreach(_ ! outgoing)

    case error: Domain.Error => println(error.message)
    case _: Domain.CreateGameFromClient => context.actorSelection("akka://RpsServer/user/gameFactory") ! CreateGameMessageFromConnectorActor(context.self)
    case event: Domain.CreateGameResponseToClient => self ! WebsocketMsg.Outgoing(CreateGameResponseToClient(gameId = event.gameId).asJson.noSpaces)
    case event: Domain.PlayerMovementMessageFromClient => context.actorSelection("akka://RpsServer/user/gameFactory/" + event.playerId) ! PlayerMovementMessageFromConnectorActor(event, self)
    case event: Domain.ResultToClient => self ! WebsocketMsg.Outgoing(event.asJson.noSpaces)
  }
}