package com.example.socket_connector.messages

trait ConnectorActorMessage

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist

sealed trait DomainClientMessage extends ConnectorActorMessage
object DomainClientMessage {
  final case class Error(message: String) extends DomainClientMessage
  final case class CreateGameFromClient() extends DomainClientMessage
  final case class PlayerMovementMessageFromClient(movement: String, gameId: String, playerId: String) extends DomainClientMessage
  final case class ListingResponse(listing: Receptionist.Listing) extends DomainClientMessage
}

sealed trait DomainClientResponse extends ConnectorActorMessage
object DomainClientResponse {
  final case class CreateGameResponseToClient(gameId: String, messageType: String = "createGameResponse") extends DomainClientResponse
  final case class ResultToClient(moves: Map[String, String], winner: String, messageType: String = "gameResult") extends DomainClientResponse
}

sealed trait WebsocketMsg extends ConnectorActorMessage
object WebsocketMsg {
  case class Connected(userActor: ActorRef[WebsocketMsg]) extends WebsocketMsg
  case class Incoming(text: String) extends WebsocketMsg
  case class Outgoing(text: String) extends WebsocketMsg
}

