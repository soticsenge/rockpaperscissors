package com.example

import akka.actor.typed.ActorRef

sealed trait MoveType {
  val userFriendlyName: String;
  val beats: Seq[MoveType]
}

case object Rock extends MoveType {
  val userFriendlyName = "Rock";
  val beats = Seq(Scissors)
}

case object Paper extends MoveType {
  val userFriendlyName = "Paper";
  val beats = Seq(Rock)
}

case object Scissors extends MoveType {
  val userFriendlyName = "Scissors";
  val beats = Seq(Paper)
}

sealed trait PlayerMovement

final case class PlayerMovementMessage(ref: ActorRef[Domain], movement: MoveType) extends PlayerMovement
final case class JoinGameRequest(connectionHandler: ActorRef[Domain]) extends PlayerMovement

