package eiti.sag.knowledge_agents

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import eiti.sag.MainApp
import eiti.sag.knowledge_agents.KnowledgeAgent.{FetchedAlreadyLearnedAnimals, LearnAbout}
import eiti.sag.knowledge_agents.KnowledgeAgentsSupervisor.{InitAgents, StartLearning}
import eiti.sag.query.UsersQueryInstance

class KnowledgeAgentsSupervisor extends Actor {

  var knowledgeAgentList: List[ActorRef] = List()

  override def receive: Receive = {
    case StartLearning() => startLearning()
    case q: UsersQueryInstance => askAQuestion(q)
    case InitAgents() => initAgents()
    case _ => println("Supervisor - dont know how to handle it")
  }

  def initAgents(): Unit = {
    val system = ActorSystem(MainApp.AnimalsKnowledgeSystemName)

    val KnowledgeAgentAFS = system.actorOf(Props[KnowledgeAgentAFS], name = "KnowledgeAgentAFS")
    val KnowledgeAgentWikipedia = system.actorOf(Props[KnowledgeAgentWikipedia], name = "KnowledgeAgentWikipedia")
    val KnowledgeAgentWWF = system.actorOf(Props[KnowledgeAgentWWF], name = "KnowledgeAgentWWF")

    knowledgeAgentList = KnowledgeAgentAFS :: knowledgeAgentList
    knowledgeAgentList = KnowledgeAgentWikipedia :: knowledgeAgentList
    knowledgeAgentList = KnowledgeAgentWWF :: knowledgeAgentList

    for (elem <- knowledgeAgentList) {
      elem ! FetchedAlreadyLearnedAnimals()
    }
  }

  def startLearning(): Unit = {
    val system = ActorSystem(MainApp.AnimalsKnowledgeSystemName)

    val KnowledgeAgentAFS = system.actorOf(Props[KnowledgeAgentAFS], name = "KnowledgeAgentAFS")
    val KnowledgeAgentWikipedia = system.actorOf(Props[KnowledgeAgentWikipedia], name = "KnowledgeAgentWikipedia")
    val KnowledgeAgentWWF = system.actorOf(Props[KnowledgeAgentWWF], name = "KnowledgeAgentWWF")

    knowledgeAgentList = KnowledgeAgentAFS :: knowledgeAgentList
    knowledgeAgentList = KnowledgeAgentWikipedia :: knowledgeAgentList
    knowledgeAgentList = KnowledgeAgentWWF :: knowledgeAgentList

    // TODO - mocked
    val animals = List("tiger", "koala", "spider", "parrot")
    for (elem <- animals) {
      KnowledgeAgentAFS ! LearnAbout(elem)
      KnowledgeAgentWikipedia ! LearnAbout(elem)
      KnowledgeAgentWWF ! LearnAbout(elem)
    }
  }

  def askAQuestion(userQuery: UsersQueryInstance): Unit = {
    for (elem <- knowledgeAgentList) {
      elem ! userQuery
    }
  }
}

object KnowledgeAgentsSupervisor {
  final case class StartLearning()
  final case class InitAgents()
}
