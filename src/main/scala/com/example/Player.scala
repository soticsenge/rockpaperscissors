package com.example

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.{ActorRef => ClassicActorRef}
import com.example.Domain.{GameResult, GameStart, PlayerMovementMessageFromConnectorActor, ResultToClient}

object Player {

  def apply(): Behavior[Domain] = waitForGameStart()

  private def waitForGameStart(): Behavior[Domain] = {
    Behaviors.receive {
      case (ctx, GameStart(game: ActorRef[PlayerMovement])) =>
        ctx.log.info("Select a move for player " + ctx.self.path)
        waitForMove(game)
      case _ =>
        Behaviors.unhandled
    }
  }

  private def waitForMove(game: ActorRef[PlayerMovement]): Behavior[Domain] = {
    Behaviors.receive {
      case (ctx, PlayerMovementMessageFromConnectorActor(move, connectionHandler)) =>
        ctx.log.info("Select a move for player" + ctx.self.path.name)
        game ! PlayerMovementMessage(ctx.self, move)
        waitForResult(game, connectionHandler)
      case _ =>
        Behaviors.unhandled
    }
  }

  private def waitForResult(game: ActorRef[PlayerMovement], connectionHandler: ClassicActorRef): Behavior[Domain] = {
    Behaviors.receivePartial {
      case (ctx, GameResult(moves, winnerId)) =>
        ctx.log.info("The result is : " + winnerId)
        connectionHandler ! ResultToClient(moves.map{case (k,v) => k -> v.userFriendlyName}, winnerId)
        waitForMove(game)
    }
  }
}