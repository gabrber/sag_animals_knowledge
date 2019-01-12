package eiti.sag

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.pattern.ask

object MainApp extends App {

  //greetings()
  val system = ActorSystem("AnimalsKnowledgeBase")
  val TranslationAgent1 = system.actorOf(Props[TranslationAgent], name = "SystemUserAgent1")
  val KnowledgeAgent1 = system.actorOf(Props[KnowledgeAgent], name = "KnowledgeAgent1")
  val KnowledgeAgent2 = system.actorOf(Props[KnowledgeAgent], name = "KnowledgeAgent2")

  val future = TranslationAgent1 ! "greetings"


}