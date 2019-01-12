package eiti.sag

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, PoisonPill, Props, Terminated}
import akka.event.Logging

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Await

class KnowledgeAgent extends Actor {
  val log = Logging(context.system, this)

  // Receive Message cases
  def receive = {
    case "hi" ⇒ println("hello")
    case animal ⇒ log.info("received message: " + animal.toString())
    //case _      ⇒ log.info("received unknown message")
  }
}
