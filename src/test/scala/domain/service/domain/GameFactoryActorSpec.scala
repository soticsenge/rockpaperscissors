package domain.service.domain

import akka.actor.testkit.typed.Effect.Spawned
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, BehaviorTestKit}
import akka.actor.typed.scaladsl.adapter._
import com.example.domain.GameFactoryActor
import com.example.domain.messages.CreateGameMessageFromConnectorActor
import com.example.domain.service.GameResultCalculatorService
import com.example.socket_connector.messages.ConnectorActorMessage
import com.example.socket_connector.messages.DomainClientResponse.CreateGameResponseToClient
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar.mock

import java.util.UUID

class GameFactoryActorSpec
  extends AnyFlatSpec
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers {

  val gameId: UUID = UUID.fromString("89615be6-45fe-4751-aad2-b3cddc11edfa")
  val calculatorService: GameResultCalculatorService = mock[GameResultCalculatorService]
  val behaviorTestkit: BehaviorTestKit[CreateGameMessageFromConnectorActor] =
    BehaviorTestKit.apply(GameFactoryActor(calculatorService, () => gameId))
  val testKit: ActorTestKit = ActorTestKit()


  override def beforeAll(): Unit = {
    super.beforeAll()
    when(calculatorService.calculateWinner(any, any, any, any)).thenReturn("draw")
  }

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "GameFactoryActor" should "create game and respond with game id" in {
    //given
    val connector = testKit.createTestProbe[ConnectorActorMessage]()
    //when
    behaviorTestkit.run(CreateGameMessageFromConnectorActor(connector.ref.toClassic))

    //then
    behaviorTestkit.expectEffectPF({ case Spawned(_, name, _) => name.shouldEqual("player1_89615") })
    behaviorTestkit.expectEffectPF({ case Spawned(_, name, _) => name.shouldEqual("player2_89615") })
    behaviorTestkit.expectEffectPF({ case Spawned(_, name, _) => name.shouldEqual("game_89615") })
    connector.expectMessage(CreateGameResponseToClient("89615"))
  }
}