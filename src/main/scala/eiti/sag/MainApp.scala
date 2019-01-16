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
  val KnowledgeAgentAFS = system.actorOf(Props[KnowledgeAgentAFS], name = "KnowledgeAgentAFS")
  val KnowledgeAgentWikipedia = system.actorOf(Props[KnowledgeAgentWikipedia], name = "KnowledgeAgentWikipedia")
  val KnowledgeAgentWWF = system.actorOf(Props[KnowledgeAgentWWF], name = "KnowledgeAgentWWF")
  val AnimalSpeciesNamesProvider = system.actorOf(Props[AnimalSpeciesNamesProvider], name = "AnimalSpeciesNamesProvider")

  val future = TranslationAgent1 ! "greetings"

//  KnowledgeAgentWikipedia ! "tiger"
//  KnowledgeAgentWikipedia ! "whale"
//  KnowledgeAgentWikipedia ! "panda"
//  KnowledgeAgentWikipedia ! "koala"
//  KnowledgeAgentWikipedia ! "bee"
//  KnowledgeAgentWikipedia ! "hyena"

//  AnimalSpeciesNamesProvider ! "fetch"


}