package com.example.domain

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.example.domain.messages._
import com.example.socket_connector.messages.ConnectorActorMessage
import com.example.socket_connector.messages.DomainClientResponse.ResultToClient

object Player {

  def apply(): Behavior[PlayerDomainMessage] = waitForGameStart()

  private def waitForGameStart(): Behavior[PlayerDomainMessage] = {
    Behaviors.receive {
      case (ctx, GameStart(game: ActorRef[PlayerMovement])) =>
        ctx.log.info("Select a move for player " + ctx.self.path)
        waitForMove(game)
      case _ =>
        Behaviors.unhandled
    }
  }

  private def waitForMove(game: ActorRef[PlayerMovement]): Behavior[PlayerDomainMessage] = {
    Behaviors.receive {
      case (ctx, PlayerMovementMessageFromConnectorActor(move, connectionHandler: ActorRef[ConnectorActorMessage])) =>
        ctx.log.info("Select a move for player" + ctx.self.path.name)
        game ! PlayerMovementMessage(ctx.self, move)
        waitForResult(game, connectionHandler)
      case _ =>
        Behaviors.unhandled
    }
  }

  private def waitForResult(game: ActorRef[PlayerMovement], connectionHandler: ActorRef[ConnectorActorMessage]): Behavior[PlayerDomainMessage] = {
    Behaviors.receivePartial {
      case (ctx, GameResult(moves, winnerId)) =>
        ctx.log.info("The result is : " + winnerId)
        connectionHandler ! ResultToClient(moves.map { case (k, v) => k -> v.userFriendlyName }, winnerId)
        waitForMove(game)
      case _ =>
        Behaviors.unhandled
    }
  }
}
