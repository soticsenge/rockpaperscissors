package com.example.domain

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.example.domain.messages._
import com.example.domain.service.GameResultCalculatorService


object GameRoom {
  def apply(name: String,
            player1: ActorRef[PlayerDomainMessage],
            player2: ActorRef[PlayerDomainMessage],
            gameService: GameResultCalculatorService,
           ): Behavior[PlayerMovement] = {
    Behaviors.setup { ctx =>
      new GameRoom(ctx, player1, player2, gameService).startGame
    }
  }
}

class GameRoom(ctx: ActorContext[PlayerMovement],
               player1: ActorRef[PlayerDomainMessage],
               player2: ActorRef[PlayerDomainMessage],
               gameService: GameResultCalculatorService,
              ) {

  private lazy val startGame: Behavior[PlayerMovement] = {
    player1 ! GameStart(ctx.self)
    player2 ! messages.GameStart(ctx.self)
    waitingForFirstMove
  }

  private lazy val waitingForFirstMove: Behavior[PlayerMovement] =
    Behaviors.receiveMessagePartial {
      case PlayerMovementMessage(`player1`, move) =>
        ctx.log.info(`player1`.path.name + " selected " + move + " as first")
        waitForSecondMove(player2, `player1`, move)
      case PlayerMovementMessage(`player2`, move) =>
        ctx.log.info(`player2`.path.name + " selected " + move)
        waitForSecondMove(player1, player2, move)
    }

  private def waitForSecondMove(
                                 playerToWaitFor: ActorRef[GameResult],
                                 playerRecieved: ActorRef[GameResult],
                                 recievedMove: MoveType
                               ): Behavior[PlayerMovement] = {
    Behaviors.receiveMessagePartial {
      case PlayerMovementMessage(`playerToWaitFor`, move2) =>
        ctx.log.info(`playerToWaitFor`.path.name + " selected " + move2 + " as second")
        ctx.log.info("Result: " + gameService.calculateWinner(playerRecieved.path.name, recievedMove, playerToWaitFor.path.name, move2))
        val winnerId = gameService.calculateWinner(playerRecieved.path.name, recievedMove, playerToWaitFor.path.name, move2)
        player1 ! GameResult(Map(playerToWaitFor.path.name -> move2, playerRecieved.path.name ->  recievedMove), winnerId)
        player2 ! GameResult(Map(playerToWaitFor.path.name -> move2, playerRecieved.path.name ->  recievedMove), winnerId)
        waitingForFirstMove
    }
  }
}

