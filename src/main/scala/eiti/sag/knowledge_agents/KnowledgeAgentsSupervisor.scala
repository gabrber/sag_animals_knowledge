package eiti.sag.knowledge_agents

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import eiti.sag.MainApp
import eiti.sag.knowledge_agents.KnowledgeAgent.{FetchedAlreadyLearnedAnimals, LearnAbout}
import eiti.sag.knowledge_agents.KnowledgeAgentsSupervisor.{InitAgents, KillAgent, StartLearning}
import eiti.sag.query.UsersQueryInstance

class KnowledgeAgentsSupervisor extends Actor {


  var knowledgeAgentMap: scala.collection.mutable.Map[String, ActorRef] = scala.collection.mutable.Map()

  def killSomeAgentAtRandom(agentName: String): Unit = {
    knowledgeAgentMap(agentName) ! PoisonPill
  }

  override def receive: Receive = {
    case StartLearning() => startLearning()
    case q: UsersQueryInstance => askAQuestion(q)
    case InitAgents() => initAgents()
    case KillAgent(agentName: String) => killSomeAgentAtRandom(agentName)
    case _ => println("Supervisor - dont know how to handle it")
  }

  def initAgents(): Unit = {
    val system = ActorSystem(MainApp.AnimalsKnowledgeSystemName)

    val KnowledgeAgentAFS = system.actorOf(Props[KnowledgeAgentAFS], name = "KnowledgeAgentAFS")
    val KnowledgeAgentWikipedia = system.actorOf(Props[KnowledgeAgentWikipedia], name = "KnowledgeAgentWikipedia")
    val KnowledgeAgentWWF = system.actorOf(Props[KnowledgeAgentWWF], name = "KnowledgeAgentWWF")

    knowledgeAgentMap.put("KnowledgeAgentAFS", KnowledgeAgentAFS)
    knowledgeAgentMap.put("KnowledgeAgentWikipedia", KnowledgeAgentWikipedia)
    knowledgeAgentMap.put("KnowledgeAgentWWF", KnowledgeAgentWWF)

    for (elem <- knowledgeAgentMap.values) {
      elem ! FetchedAlreadyLearnedAnimals()
    }
  }

  def startLearning(): Unit = {
    val system = ActorSystem(MainApp.AnimalsKnowledgeSystemName)

    val KnowledgeAgentAFS = system.actorOf(Props[KnowledgeAgentAFS], name = "KnowledgeAgentAFS")
    val KnowledgeAgentWikipedia = system.actorOf(Props[KnowledgeAgentWikipedia], name = "KnowledgeAgentWikipedia")
    val KnowledgeAgentWWF = system.actorOf(Props[KnowledgeAgentWWF], name = "KnowledgeAgentWWF")

    knowledgeAgentMap.put("KnowledgeAgentAFS", KnowledgeAgentAFS)
    knowledgeAgentMap.put("KnowledgeAgentWikipedia", KnowledgeAgentWikipedia)
    knowledgeAgentMap.put("KnowledgeAgentWWF", KnowledgeAgentWWF)

    // TODO - mocked
    val animals = List("tiger", "koala", "spider", "parrot")
    for (elem <- animals) {
      KnowledgeAgentAFS ! LearnAbout(elem)
      KnowledgeAgentWikipedia ! LearnAbout(elem)
      KnowledgeAgentWWF ! LearnAbout(elem)
    }
  }

  def askAQuestion(userQuery: UsersQueryInstance): Unit = {
    for (elem <- knowledgeAgentMap.values) {
      elem ! userQuery
    }
  }
}

object KnowledgeAgentsSupervisor {

  val AgentName: String = "KnowledgeAgentsSupervisor"
  val FullAgentPath: String = "akka://" + MainApp.AnimalsKnowledgeSystemName +  "/user/" + AgentName

  final case class StartLearning()
  final case class InitAgents()
  final case class KillAgent(name: String)
}
