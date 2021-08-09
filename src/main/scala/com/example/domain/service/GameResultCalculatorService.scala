package com.example.domain.service

import com.example.domain.messages.MoveType

class GameResultCalculatorService {
  def calculateWinner(player1Name: String, player1Move: MoveType, player2Name: String, player2Move: MoveType): String = {
    if (player1Move.beats.contains(player2Move) && !player2Move.beats.contains(player2Move)) {
      player1Name
    } else if (player2Move.beats.contains(player1Move) && !player1Move.beats.contains(player2Move)) {
      player2Name
    } else "draw"
  }
}
