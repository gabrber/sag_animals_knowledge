package eiti.sag

import akka.actor.{ActorSystem, Props}
import scala.language.postfixOps
import eiti.sag.knowledge_agents.KnowledgeAgentsSupervisor.{InitAgents, StartLearning}
import eiti.sag.knowledge_agents.{KnowledgeAgentAFS, KnowledgeAgentWWF, KnowledgeAgentWikipedia, KnowledgeAgentsSupervisor}
import eiti.sag.meta_knowledge_agents.MetaKnowledgeAgentsSupervisor

object MainApp extends App {

  val AnimalsKnowledgeSystemName = "AnimalsKnowledgeBase"

  val mode = Mode.fromString(Option(System.getProperty("Mode")).getOrElse(""))

  if(mode == null) {
    println("Error: invalid run configuration mode")
    System.exit(1)
  }


  val system = ActorSystem(AnimalsKnowledgeSystemName)

  val knowledgeAgentsSupervisor = system.actorOf(Props[KnowledgeAgentsSupervisor], name="KnowledgeAgentsSupervisor")
  val webServerAgent = system.actorOf(Props[HttpServer], name="HttpServer")

  if(mode == Mode.Learn) {
    println("learning")
    knowledgeAgentsSupervisor ! StartLearning()
    val metaKnowledgeAgentsSupervisor = system.actorOf(Props[MetaKnowledgeAgentsSupervisor], name="MetaKnowledgeAgentsSupervisor")
    metaKnowledgeAgentsSupervisor ! "fetch"
  }

  if(mode == Mode.Explore) {
    println("exploring")
    knowledgeAgentsSupervisor ! InitAgents()
    val TranslationAgent1 = system.actorOf(Props[TranslationAgent], name = "SystemUserAgent1")
    TranslationAgent1 ! "greetings"
  }



}

object Mode extends Enumeration {
  type Mode = Value
  val Learn, Explore = Value

  def fromString(str: String): Mode = {
    if(str.equals("learn")) {
      Learn
    } else if(str.equals("explore")) {
      Explore
    } else {
      null
    }
  }
}