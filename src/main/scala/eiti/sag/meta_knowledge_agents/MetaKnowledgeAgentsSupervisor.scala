package eiti.sag.meta_knowledge_agents

import akka.actor.{Actor, ActorSystem, Props}
import eiti.sag.MainApp

class MetaKnowledgeAgentsSupervisor extends Actor {

  override def receive: Receive = {
    case "start" =>
      startAgents()
  }


  def startAgents(): Unit = {
    val system = ActorSystem(MainApp.AnimalsKnowledgeSystemName)
    val animalSpeciesNamesProvider = system.actorOf(Props[AnimalSpeciesNamesProvider])



    animalSpeciesNamesProvider ! "fetch"

  }
}