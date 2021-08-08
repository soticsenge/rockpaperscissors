package com.example

import akka.actor.typed.ActorSystem
import akka.actor.{ActorSystem => ClassicActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.actor.typed.scaladsl.adapter._

import scala.util.Failure
import scala.util.Success

object QuickstartApp {
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
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      context.spawn(GameFactoryActor(),"gameFactory")
      Behaviors.empty
    }

    implicit val typesSystem = ActorSystem[Nothing](rootBehavior, "RpsServer")
    val system: ClassicActorSystem = typesSystem.toClassic
    val routes = new RpsRoutes()(system, typesSystem)
    startHttpServer(routes.websocketEventsRoute)(typesSystem)

  }
}
