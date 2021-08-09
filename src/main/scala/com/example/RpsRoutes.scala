package com.example

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.server.Directives.{handleWebSocketMessages, path}
import akka.http.scaladsl.server.Route
import com.example.socket_connector.ClientHandlerFlow

class RpsRoutes()(implicit val system: ActorSystem[_]) {
  final def websocketEventsRoute: Route = path("events") {
    val flow = new ClientHandlerFlow().websocketFlow()
    handleWebSocketMessages(flow)
  }
}


