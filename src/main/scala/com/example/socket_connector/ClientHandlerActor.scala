package com.example.socket_connector

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import com.example.domain.GameFactoryActor
import com.example.domain.messages._
import com.example.socket_connector.messages.DomainClientMessage._
import com.example.socket_connector.messages.DomainClientResponse.{CreateGameResponseToClient, ResultToClient}
import com.example.socket_connector.messages.{ConnectorActorMessage, WebsocketMsg}
import io.circe.HCursor
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.parser.parse
import io.circe.syntax.EncoderOps

object ClientHandlerActor {

  def apply(): Behavior[ConnectorActorMessage] = defaultSetup(Set.empty[ActorRef[WebsocketMsg]])

  private def defaultSetup(userActors: Set[ActorRef[WebsocketMsg]], gameMoveEvent: Option[PlayerMovementMessageFromClient] = None): Behavior[ConnectorActorMessage] = {
    val key = ServiceKey[PlayerDomainMessage](gameMoveEvent.map(_.playerId).getOrElse(""))

    Behaviors.receive[ConnectorActorMessage] {
      case (ctx, WebsocketMsg.Connected(userActor)) =>
        ctx.watch(userActor)
        defaultSetup(userActors + userActor, gameMoveEvent)
      case (ctx, WebsocketMsg.Incoming(text)) =>
        parseAndHandleClientMessage(ctx, text)
      case (_, outgoing: WebsocketMsg.Outgoing) =>
        userActors.foreach(_ ! outgoing)
        Behaviors.same

      case (ctx, _: CreateGameFromClient) =>
        val listingResponseAdapter = ctx.messageAdapter[Receptionist.Listing](ListingResponse)
        ctx.system.receptionist ! Receptionist.Find(GameFactoryActor.GameFactoryActorKey, listingResponseAdapter)
        Behaviors.same
      case (ctx, ListingResponse(GameFactoryActor.GameFactoryActorKey.Listing(listings))) =>
        listings.foreach(ps => ps ! CreateGameMessageFromConnectorActor(ctx.self))
        Behaviors.same
      case (ctx, event: CreateGameResponseToClient) =>
        ctx.self ! WebsocketMsg.Outgoing(CreateGameResponseToClient(gameId = event.gameId).asJson.noSpaces)
        Behaviors.same

      case (ctx, event: PlayerMovementMessageFromClient) =>
        val listingResponseAdapter = ctx.messageAdapter[Receptionist.Listing](ListingResponse)
        ctx.system.receptionist ! Receptionist.Find(ServiceKey[PlayerDomainMessage](event.playerId), listingResponseAdapter)
        defaultSetup(userActors, Some(event))
      case (ctx, ListingResponse(key.Listing(listings))) =>
        listings.foreach(ps => ps ! PlayerMovementMessageFromConnectorActor(gameMoveEvent.get, ctx.self))
        defaultSetup(userActors)
      case (ctx, event: ResultToClient) =>
        ctx.self ! WebsocketMsg.Outgoing(event.asJson.noSpaces)
        Behaviors.same

      case (ctx, error: Error) =>
        ctx.log.error(error.message)
        ctx.self ! WebsocketMsg.Outgoing(error.message)
        Behaviors.same
      case (ctx, e) =>
        ctx.log.error(e.toString)
        ctx.self ! Error(s"Unexpected message: " + e.toString)
        Behaviors.same
    }
      .receiveSignal {
        case (ctx, Terminated(userActor)) =>
          val openUserActors: Set[ActorRef[WebsocketMsg]] = userActors - userActor.asInstanceOf[ActorRef[WebsocketMsg]]
          if (openUserActors.isEmpty) {
            Behaviors.stopped
          } else {
            ctx.unwatch(userActor)
            defaultSetup(openUserActors, gameMoveEvent)
          }
      }
  }

  private def parseAndHandleClientMessage(ctx: ActorContext[ConnectorActorMessage], text: String): Behavior[ConnectorActorMessage] = {
    parse(StringContext.processEscapes(text.slice(1, text.length - 1))) match {
      case Left(_) =>
        ctx.self ! Error(s"Cannot parse $text as JSON")
        Behaviors.same
      case Right(value) =>
        val cursor: HCursor = value.hcursor
        cursor.get[String]("messageType") match {
          case Right("createGameRequest") =>
            value.as[CreateGameFromClient] match {
              case Left(_) =>
                ctx.self ! Error(s"Cannot parse $value as CreateGameFromClient")
                Behaviors.same
              case Right(event) =>
                ctx.self ! event
                Behaviors.same
            }
          case Right("gameMoveRequest") =>
            value.as[PlayerMovementMessageFromClient] match {
              case Left(_) =>
                ctx.self ! Error(s"Cannot parse $value as PlayerMovementMessageFromClient")
                Behaviors.same
              case Right(event) =>
                ctx.self ! event
                Behaviors.same
            }
          case _ => ctx.self ! Error(s"Cannot parse $text as Domain - invalid message")
            Behaviors.same
        }
    }
  }
}
