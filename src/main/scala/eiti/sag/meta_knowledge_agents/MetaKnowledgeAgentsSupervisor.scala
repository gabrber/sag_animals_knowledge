package eiti.sag.meta_knowledge_agents

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import eiti.sag.MainApp
import eiti.sag.meta_knowledge_agents.MetaKnowledgeAgentsSupervisor.{AskForAnimalSpecies, FindAnimalSpeciesToLearn}

class MetaKnowledgeAgentsSupervisor extends Actor {

  var animalSpeciesNamesProvider: ActorRef = null

  override def receive: Receive = {
    case AskForAnimalSpecies(animalsLearnedAbout) =>
      val replyTo = sender()
      if(animalSpeciesNamesProvider == null) {
        startAgents()
      }
      animalSpeciesNamesProvider ! FindAnimalSpeciesToLearn(replyTo, animalsLearnedAbout)
    case _ => println("MetaKnowledgeAgentsSupervisor - unknown message received")
  }


  def startAgents(): Unit = {
    animalSpeciesNamesProvider = context.actorOf(Props[AnimalSpeciesNamesProvider])
    animalSpeciesNamesProvider ! "fetch"
  }
}

object MetaKnowledgeAgentsSupervisor {
  final case class AskForAnimalSpecies(animalsLearnedAbout: List[String])
  final case class FindAnimalSpeciesToLearn(sendTo: ActorRef, animalsLearnedAbout: List[String])
}