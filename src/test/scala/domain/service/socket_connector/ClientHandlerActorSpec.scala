package domain.service.socket_connector

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, BehaviorTestKit}
import akka.actor.typed.Terminated
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import com.example.domain.GameFactoryActor
import com.example.domain.messages._
import com.example.socket_connector.ClientHandlerActor
import com.example.socket_connector.messages.DomainClientResponse.ResultToClient
import com.example.socket_connector.messages.WebsocketMsg.Incoming
import com.example.socket_connector.messages.{ConnectorActorMessage, WebsocketMsg}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import java.util.UUID

class ClientHandlerActorSpec
  extends AnyFlatSpec
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers {

  val testKit: ActorTestKit = ActorTestKit()
  val gameId: UUID = UUID.fromString("89615be6-45fe-4751-aad2-b3cddc11edfa")

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "ClientHandlerActor" should "handle invalid JSON message" in {
    //given
    val socketHandler = testKit.createTestProbe[WebsocketMsg]()
    val gameFactory = testKit.createTestProbe[CreateGameMessageFromConnectorActor]()
    val connector = testKit.spawn[ConnectorActorMessage](ClientHandlerActor())
    connector ! WebsocketMsg.Connected(socketHandler.ref)

    //when
    connector ! Incoming("\"{\"messageType\":\"gameMoveRequest\"\"")
    //then
    socketHandler expectMessage WebsocketMsg.Outgoing("Cannot parse \"{\"messageType\":\"gameMoveRequest\"\" as JSON")
  }

  "ClientHandlerActor" should "handle not existing message" in {
    //given
    val socketHandler = testKit.createTestProbe[WebsocketMsg]()
    val gameFactory = testKit.createTestProbe[CreateGameMessageFromConnectorActor]()
    val connector = testKit.spawn[ConnectorActorMessage](ClientHandlerActor())
    connector ! WebsocketMsg.Connected(socketHandler.ref)

    //when
    connector ! Incoming("\"{\"messageType\":\"notExisting\"}\"")
    //then
    socketHandler expectMessage WebsocketMsg.Outgoing("Cannot parse \"{\"messageType\":\"notExisting\"}\" as Domain - invalid message")
  }

  "ClientHandlerActor" should "handle valid create game message" in {
    //given
    val gameFactory = testKit.createTestProbe[CreateGameMessageFromConnectorActor]()
    testKit.internalSystem.receptionist ! Receptionist.Register(GameFactoryActor.GameFactoryActorKey, gameFactory.ref)
    val connector = testKit.spawn[ConnectorActorMessage](ClientHandlerActor())
    //when
    connector ! Incoming("\"{\"messageType\":\"createGameRequest\"}\"")
    //then
    gameFactory expectMessage CreateGameMessageFromConnectorActor(connector.ref)
  }

  "ClientHandlerActor" should "handle valid game move message" in {
    //given
    val player = testKit.createTestProbe[PlayerDomainMessage]()
    testKit.internalSystem.receptionist ! Receptionist.Register(ServiceKey[PlayerDomainMessage]("player1_8961"), player.ref)
    val connector = testKit.spawn[ConnectorActorMessage](ClientHandlerActor())

    //when
    connector ! Incoming("\"{\"messageType\":\"gameMoveRequest\", \"movement\":\"S\", \"gameId\":\"game_8961\", \"playerId\":\"player1_8961\"}\"")
    //then
    player expectMessage PlayerMovementMessageFromConnectorActor(Scissors, connector.ref)
  }

  "ClientHandlerActor" should "forward result to client" in {
    //given
    val socketHandler = testKit.createTestProbe[WebsocketMsg]()
    val gameFactory = testKit.createTestProbe[CreateGameMessageFromConnectorActor]()
    val connector = testKit.spawn[ConnectorActorMessage](ClientHandlerActor())
    connector ! WebsocketMsg.Connected(socketHandler.ref)

    //when
    connector ! ResultToClient(Map(), "test_winner_id")
    //then
    socketHandler expectMessage WebsocketMsg.Outgoing("{\"moves\":{},\"winner\":\"test_winner_id\",\"messageType\":\"gameResult\"}")
  }

  "ClientHandlerActor" should "terminate if socket terminates" in {
    //given
    val socketHandler = testKit.createTestProbe[WebsocketMsg]()
    val gameFactory = testKit.createTestProbe[CreateGameMessageFromConnectorActor]()
    val behaviorTestkit = BehaviorTestKit.apply(ClientHandlerActor())
    behaviorTestkit.run(WebsocketMsg.Connected(socketHandler.ref))
    //when
    behaviorTestkit.signal(Terminated(socketHandler.ref))
    //then
    behaviorTestkit.currentBehavior.shouldBe(Behaviors.stopped)
  }
}