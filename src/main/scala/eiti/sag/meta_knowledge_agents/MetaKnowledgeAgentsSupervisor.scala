package eiti.sag.meta_knowledge_agents

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import eiti.sag.MainApp
import eiti.sag.meta_knowledge_agents.MetaKnowledgeAgentsSupervisor.{AskForAnimalSpecies, FindAnimalSpeciesToLearn}

class MetaKnowledgeAgentsSupervisor extends Actor {

  var animalSpeciesNamesProvider: ActorRef = null

  override def receive: Receive = {
    case "start" =>
      println(self.path.toString)
      startAgents()
    case AskForAnimalSpecies(animalsLearnedAbout) =>
      println("MetaKnowledgeAgentsSupervisor - AskForAnimalSpecies")
      val replyTo = sender()
      animalSpeciesNamesProvider ! FindAnimalSpeciesToLearn(replyTo, animalsLearnedAbout)
    case _ => println("MetaKnowledgeAgentsSupervisor cannot")
  }


  def startAgents(): Unit = {
    val system = ActorSystem(MainApp.AnimalsKnowledgeSystemName)
    animalSpeciesNamesProvider = context.actorOf(Props[AnimalSpeciesNamesProvider])



    animalSpeciesNamesProvider ! "fetch"

  }
}

object MetaKnowledgeAgentsSupervisor {
  final case class AskForAnimalSpecies(animalsLearnedAbout: List[String])
  final case class FindAnimalSpeciesToLearn(sendTo: ActorRef, animalsLearnedAbout: List[String])
}