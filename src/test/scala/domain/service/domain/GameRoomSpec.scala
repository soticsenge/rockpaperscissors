package domain.service.domain

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import com.example.domain.messages._
import com.example.domain.service.GameResultCalculatorService
import com.example.domain.{GameRoom, messages}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar.mock

import java.util.UUID

class GameRoomSpec
  extends AnyFlatSpec
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers {

  val testKit: ActorTestKit = ActorTestKit()
  val calculatorService: GameResultCalculatorService = mock[GameResultCalculatorService]

  override def beforeAll(): Unit = {
    super.beforeAll()
    when(calculatorService.calculateWinner(any, any, any, any)).thenReturn("draw")
  }

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "GameRoom" should "send join message to players on startup" in {
    //given
    val uuid = UUID.randomUUID().toString
    val player1 = testKit.createTestProbe[PlayerDomainMessage]()
    val player2 = testKit.createTestProbe[PlayerDomainMessage]()
    val gameRoom = testKit.spawn(GameRoom(s"testGameRoom-$uuid", player1.ref, player2.ref, calculatorService), s"testGameRoom-$uuid")
    //then
    player1.expectMessage(GameStart(gameRoom))
    player2.expectMessage(messages.GameStart(gameRoom))
  }

  "GameRoom" should "respond with result when player1 moves first" in {
    //given
    val uuid = UUID.randomUUID().toString
    val player1 = testKit.createTestProbe[PlayerDomainMessage]("player1")
    val player2 = testKit.createTestProbe[PlayerDomainMessage]("player2")
    val gameRoom = testKit.spawn(GameRoom(s"testGameRoom-$uuid", player1.ref, player2.ref, calculatorService), s"testGameRoom-$uuid")
    //then
    player1.expectMessage(messages.GameStart(gameRoom))
    player2.expectMessage(messages.GameStart(gameRoom))
    //when
    gameRoom ! PlayerMovementMessage(player1.ref, Rock)
    gameRoom ! PlayerMovementMessage(player2.ref, Rock)
    //then
    player1.expectMessage(GameResult(Map(player1.ref.path.name -> Rock, player2.ref.path.name -> Rock), "draw"))
    player2.expectMessage(GameResult(Map(player1.ref.path.name -> Rock, player2.ref.path.name -> Rock), "draw"))
  }

  "GameRoom" should "respond with result when player2 moves first" in {
    //given
    val uuid = UUID.randomUUID().toString
    val player1 = testKit.createTestProbe[PlayerDomainMessage]("player1")
    val player2 = testKit.createTestProbe[PlayerDomainMessage]("player2")
    val gameRoom = testKit.spawn(GameRoom(s"testGameRoom-$uuid", player1.ref, player2.ref, calculatorService), s"testGameRoom-$uuid")
    //then
    player1.expectMessage(messages.GameStart(gameRoom))
    player2.expectMessage(messages.GameStart(gameRoom))
    //when
    gameRoom ! PlayerMovementMessage(player2.ref, Rock)
    gameRoom ! PlayerMovementMessage(player1.ref, Rock)
    //then
    player1.expectMessage(GameResult(Map(player1.ref.path.name -> Rock, player2.ref.path.name -> Rock), "draw"))
    player2.expectMessage(GameResult(Map(player1.ref.path.name -> Rock, player2.ref.path.name -> Rock), "draw"))
  }
}