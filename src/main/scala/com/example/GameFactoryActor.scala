package com.example

import akka.actor.TypedActor.context
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.adapter.ClassicActorContextOps
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.{ActorRef => ClassicActorRef}
import com.example.Domain.CreateGameResponseToClient

import java.util.UUID

object GameFactoryActor {
  def apply(): Behavior[Domain] = {
    Behaviors.setup { ctx: ActorContext[Domain] =>
      new GameFactoryActor(ctx).serveRequests
    }
  }
}

class GameFactoryActor(ctx: ActorContext[Domain]) {

  private lazy val serveRequests: Behavior[Domain] =
    Behaviors.receiveMessagePartial {
      case Domain.CreateGameMessageFromConnectorActor(sender: ClassicActorRef) =>
        val gameId = UUID.randomUUID().toString.slice(0, 5)
        val players = for (i <- 1 to 2)
          yield ctx.spawn(Player(), "player" + i + "_" + gameId)
        ctx.spawn(GameRoom("game_" + gameId, players(0), players(1)), "game" + gameId)
        sender ! CreateGameResponseToClient(gameId)
        Behaviors.same
    }
}
