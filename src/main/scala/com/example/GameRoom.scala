package com.example

import akka.Done
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.example.Domain.{GameResult, GameStart}

import scala.concurrent.Future

object GameRoom {
  def apply(name: String,
            player1: ActorRef[Domain],
            player2: ActorRef[Domain],
           ): Behavior[PlayerMovement] = {
    Behaviors.setup { ctx =>
      new GameRoom(ctx, player1, player2).startGame
    }
  }
}

class GameRoom(ctx: ActorContext[PlayerMovement],
               player1: ActorRef[Domain],
               player2: ActorRef[Domain],
              ) {

  private lazy val startGame: Behavior[PlayerMovement] = {
    player1 ! GameStart(ctx.self)
    player2 ! GameStart(ctx.self)
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
      case PlayerMovementMessage(`playerToWaitFor`, move2) => {
        ctx.log.info(`playerToWaitFor`.path.name + " selected " + move2 + " as second")
        ctx.log.info("Result: " + calculateWinner(playerRecieved.path.name, recievedMove, playerToWaitFor.path.name, move2))
        val winnerId = calculateWinner(playerRecieved.path.name, recievedMove, playerToWaitFor.path.name, move2)
        player1 ! GameResult(Map(playerToWaitFor.path.name -> move2, playerRecieved.path.name ->  recievedMove), winnerId)
        player2 ! GameResult(Map(playerToWaitFor.path.name -> move2, playerRecieved.path.name ->  recievedMove), winnerId)
        waitingForFirstMove
      }
    }
  }

  private def calculateWinner(player1Name: String, player1Move: MoveType, player2Name: String, player2Move: MoveType): String = {
    if ((player1Move.beats.contains(player2Move)) && !(player2Move.beats.contains(player2Move))) {
      player1Name
    } else if ((player2Move.beats.contains(player1Move)) && !(player1Move.beats.contains(player2Move))) {
      player2Name
    } else "draw"
  }
}

