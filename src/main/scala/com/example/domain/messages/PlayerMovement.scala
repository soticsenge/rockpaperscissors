package com.example.domain.messages

import akka.actor.typed.ActorRef

sealed trait PlayerMovement
final case class PlayerMovementMessage(ref: ActorRef[PlayerDomainMessage], movement: MoveType) extends PlayerMovement
