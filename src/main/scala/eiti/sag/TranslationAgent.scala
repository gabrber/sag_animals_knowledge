package eiti.sag

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, PoisonPill, Props, Terminated}
import akka.event.Logging

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Await

class TranslationAgent extends Actor {
  val log = Logging(context.system, this)

  // Get animal name from user
  def greetings(): String = {
    println("Hello! Which animal are you interested in?")
    var animal = scala.io.StdIn.readLine()
    println("Okay. Looking for information about " + animal)
    return animal
  }

  def getInfoType(animal:String): String = {
    println("What do you want to know about " + animal + "?")
    var infoType = scala.io.StdIn.readLine()
    println("Okay. Looking for " + infoType)
    return infoType
  }

  // Choose one Agent with name matching pattern
  def choseOneAgent(patternName:String): ActorRef = {
    var test = context.actorSelection("akka://AnimalsKnowledgeBase/user/" + patternName + "*").resolveOne(5 second)
    var agent = Await.result(test,5 second)
    return agent
  }

  // Choose all Agents with name matching pattern
  def choseAllAgents(patternName:String): ActorSelection = {
    var agent = context.actorSelection("akka://AnimalsKnowledgeBase/user/" + patternName + "*")
    return agent
  }

  // Receive Message cases
  def receive = {
    case "greetings" ⇒
      var animal = greetings()
      var infoType = getInfoType(animal)
      //choseAllAgents("KnowledgeAgent") ! "hi"
      choseAllAgents("KnowledgeAgent") ! animal
    case _      ⇒ log.info("received unknown message")
  }
}
