package domain.service.domain

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.RecipientRef.RecipientRefOps
import akka.actor.typed.scaladsl.adapter._
import com.example.domain.messages._
import com.example.domain.{Player, messages}
import com.example.socket_connector.messages.ConnectorActorMessage
import com.example.socket_connector.messages.DomainClientResponse.ResultToClient
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class PlayerSpec
  extends AnyFlatSpec
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers {

  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "Player" should "forward moves to game actor" in {
    //given
    val connector = testKit.createTestProbe[PlayerDomainMessage]().ref.toClassic
    val game = testKit.createTestProbe[PlayerMovement]()
    val player = testKit.spawn[PlayerDomainMessage](Player())
    //when
    player ! GameStart(game.ref)
    player ! PlayerMovementMessageFromConnectorActor(Rock, connector.ref)
    //then
    game.expectMessage(PlayerMovementMessage(player, Rock))
  }
  "Player" should "forward result to connection handler actor" in {
    //given
    val connector = testKit.createTestProbe[ConnectorActorMessage]()
    val game = testKit.createTestProbe[PlayerMovement]()
    val player = testKit.spawn(Player())
    //when
    player ! messages.GameStart(game.ref)
    player ! messages.PlayerMovementMessageFromConnectorActor(Rock, connector.ref.toClassic)
    player ! GameResult(Map(), "draw")
    //then
    connector.expectMessage(ResultToClient(Map(), "draw"))
  }
}