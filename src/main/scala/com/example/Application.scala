package com.example

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.example.domain.GameFactoryActor
import com.example.domain.messages.CreateGameMessageFromConnectorActor
import com.example.domain.service.GameResultCalculatorService

import java.util.UUID
import scala.util.{Failure, Success}

object Application {
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext
    val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
  def main(args: Array[String]): Unit = {
    val gameService = new GameResultCalculatorService()
    var gameFactoryActor: ActorRef[CreateGameMessageFromConnectorActor] = null
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      gameFactoryActor = context.spawn(GameFactoryActor(gameService, () => UUID.randomUUID()),"gameFactory")
      Behaviors.empty
    }

    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](rootBehavior, "RpsServer")
    val routes = new RpsRoutes()(system)
    startHttpServer(routes.websocketEventsRoute)(system)
  }
}
