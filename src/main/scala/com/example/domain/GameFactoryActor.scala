package com.example.domain

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.example.domain.messages.{CreateGameMessageFromConnectorActor, PlayerDomainMessage}
import com.example.domain.service.GameResultCalculatorService
import com.example.socket_connector.messages.DomainClientResponse.CreateGameResponseToClient

import java.util.UUID

object GameFactoryActor {
  val GameFactoryActorKey: ServiceKey[CreateGameMessageFromConnectorActor] = ServiceKey[CreateGameMessageFromConnectorActor]("gameFactoryActor")

  def apply(gameService: GameResultCalculatorService, uuidGen: () => UUID): Behavior[CreateGameMessageFromConnectorActor] = {
    Behaviors.setup { ctx: ActorContext[CreateGameMessageFromConnectorActor] =>
      ctx.system.receptionist ! Receptionist.Register(GameFactoryActorKey, ctx.self)
      new GameFactoryActor(ctx, gameService, uuidGen).serveRequests
    }
  }
}

class GameFactoryActor(ctx: ActorContext[CreateGameMessageFromConnectorActor], gameService: GameResultCalculatorService, uuidGen: () => UUID) {
  private lazy val serveRequests: Behavior[CreateGameMessageFromConnectorActor] =
    Behaviors.receiveMessagePartial {
      case CreateGameMessageFromConnectorActor(sender: ActorRef[CreateGameResponseToClient]) =>
        val gameId = uuidGen().toString.slice(0, 5)
        ctx.log.info("Created game and players with game id:" + gameId)
        val players = for (i <- 1 to 2)
          yield {
            val player = ctx.spawn(Player(), "player" + i + "_" + gameId)
            ctx.system.receptionist ! Receptionist.Register(ServiceKey[PlayerDomainMessage]("player" + i + "_" + gameId), player.ref)
            player
          }
        val game = ctx.spawn(GameRoom("game_" + gameId, players(0), players(1), gameService), "game_" + gameId)

        ctx.system.receptionist ! Receptionist.Register(ServiceKey("game_" + gameId), game.ref)
        sender ! CreateGameResponseToClient(gameId)
        Behaviors.same
    }
}
