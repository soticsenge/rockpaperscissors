package com.example

import akka.actor.typed.ActorSystem
import akka.actor.{ActorSystem => ClassicActorSystem}
import akka.http.scaladsl.server.Directives.{handleWebSocketMessages, path}
import akka.http.scaladsl.server.Route

class RpsRoutes()(implicit val system: ClassicActorSystem, implicit val typedSystem: ActorSystem[_]) {
  final def websocketEventsRoute: Route = path("events") {
    val flow = new ClientHandlerFlow().websocketFlow()
    handleWebSocketMessages(flow)
  }
}


