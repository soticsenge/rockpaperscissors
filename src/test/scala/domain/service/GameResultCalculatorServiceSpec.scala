package domain.service

import com.example.domain.messages.{MoveType, Paper, Rock, Scissors}
import com.example.domain.service.GameResultCalculatorService
import org.scalatest.Inspectors.forAll
import org.scalatest.flatspec._
import org.scalatest.matchers._

class GameResultCalculatorServiceSpec extends AnyFlatSpec with should.Matchers {
  
  val underTest = new GameResultCalculatorService()

  val expectedResults: Map[(MoveType, MoveType), String] = Map(
    (Rock, Paper) -> "player2",
    (Rock, Scissors) -> "player1",
    (Rock, Rock) -> "draw",
    (Paper, Paper) -> "draw",
    (Paper, Scissors) -> "player2",
    (Paper, Rock) -> "player1",
    (Rock, Paper) -> "player2",
    (Rock, Scissors) -> "player1",
    (Rock, Rock) -> "draw",
  )


  "GameResultCalculatorServiceSpec" should "calculate results correclty" in {
    forAll (expectedResults) { moves => {
      underTest.calculateWinner("player1", moves._1._1, "player2", moves._1._2).shouldEqual(moves._2)
      }
    }
  }
}