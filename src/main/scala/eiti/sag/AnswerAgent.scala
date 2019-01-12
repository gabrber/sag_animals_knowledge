package eiti.sag

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, PoisonPill, Props, Terminated}
import akka.event.Logging

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Await

class AnswerAgent extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case _      ⇒ log.info("received unknown message")
  }
}
