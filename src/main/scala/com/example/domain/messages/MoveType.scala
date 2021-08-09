package com.example.domain.messages

sealed trait MoveType {
  val userFriendlyName: String
  val beats: Seq[MoveType]
}

case object Rock extends MoveType {
  val userFriendlyName = "Rock"
  val beats = Seq(Scissors)
}

case object Paper extends MoveType {
  val userFriendlyName = "Paper"
  val beats = Seq(Rock)
}

case object Scissors extends MoveType {
  val userFriendlyName = "Scissors"
  val beats = Seq(Paper)
}



