package com.example.domain.messages

import akka.actor.typed.ActorRef
import com.example.socket_connector.messages.DomainClientMessage

sealed trait PlayerDomainMessage
final case class GameStart(game: ActorRef[PlayerMovement]) extends PlayerDomainMessage
final case class GameResult(moves: Map[String, MoveType], winnerId: String) extends PlayerDomainMessage
final case class CreateGameMessageFromConnectorActor(connectionHandler: ActorRef[_]) extends PlayerDomainMessage
final case class PlayerMovementMessageFromConnectorActor(movement: MoveType, connectionHandler: ActorRef[_]) extends PlayerDomainMessage
object PlayerMovementMessageFromConnectorActor {
  def apply(p: DomainClientMessage.PlayerMovementMessageFromClient, connectionHandler: ActorRef[_]): PlayerMovementMessageFromConnectorActor = {
    p.movement match {
      case "R" => PlayerMovementMessageFromConnectorActor(Rock, connectionHandler)
      case "P" => PlayerMovementMessageFromConnectorActor(Paper, connectionHandler)
      case "S" => PlayerMovementMessageFromConnectorActor(Scissors, connectionHandler)
    }
  }
}






