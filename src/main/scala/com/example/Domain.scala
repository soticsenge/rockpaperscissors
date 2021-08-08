package com.example
import akka.actor.typed.ActorRef
import akka.actor.{ActorRef => ClassicActorRef}

sealed trait Domain

object Domain {
  case class Error(message: String)  extends Domain
  final case class CreateGameFromClient() extends Domain
  final case class PlayerMovementMessageFromClient(movement: String, gameId: String, playerId: String) extends Domain
  final case class CreateGameMessageFromConnectorActor(connectionHandler: ClassicActorRef) extends Domain

  final case class CreateGameResponseToClient( gameId: String, messageType: String = "createGameResponse") extends Domain
  final case class ResultToClient(moves: Map[String, String], winner: String, messageType:String =  "gameResult") extends Domain

  final case class GameStart(game: ActorRef[PlayerMovement]) extends Domain
  final case class GameResult(moves: Map[String, MoveType], winnerId: String) extends Domain

  final case class PlayerMovementMessageFromConnectorActor(movement: MoveType, connectionHandler: ClassicActorRef) extends Domain
  object PlayerMovementMessageFromConnectorActor {
    def apply(p: PlayerMovementMessageFromClient, connectionHandler: ClassicActorRef): PlayerMovementMessageFromConnectorActor = {
      p.movement match {
        case "R" => PlayerMovementMessageFromConnectorActor(Rock, connectionHandler)
        case "P" => PlayerMovementMessageFromConnectorActor(Paper, connectionHandler)
        case "S" => PlayerMovementMessageFromConnectorActor(Scissors, connectionHandler)
      }
    }
  }
}

private sealed trait WebsocketMsg

private object WebsocketMsg {
  case class Connected(userActor: ClassicActorRef) extends WebsocketMsg
  case class Incoming(text: String) extends WebsocketMsg
  case class Outgoing(text: String) extends WebsocketMsg
}
