package eiti.sag.knowledge_agents

import scala.concurrent.duration._
import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, PoisonPill, Props, SupervisorStrategy}
import eiti.sag.HttpServer.Kaboom
import eiti.sag.MainApp
import eiti.sag.knowledge_agents.KnowledgeAgent.{FetchedAlreadyLearnedAnimals, LearnAbout}
import eiti.sag.knowledge_agents.KnowledgeAgentsSupervisor.{InitAgents, KillAgent, StartLearning}
import eiti.sag.query.UsersQueryInstance
import jdk.nashorn.internal.runtime.OptimisticReturnFilters

import scala.util.Random

class KnowledgeAgentsSupervisor extends Actor {


  var knowledgeAgentMap: List[ActorRef] = List()

  def killSomeAgentAtRandom(): Unit = {
    val chosenAgent = knowledgeAgentMap(new Random().nextInt(knowledgeAgentMap.size))

    println("Sending Kaboom to: " + chosenAgent.path)

    chosenAgent ! Kaboom
  }

  override def receive: Receive = {
    case StartLearning(animals) => startLearning(animals)
    case q: UsersQueryInstance => askAQuestion(q)
    case InitAgents() => initAgents()
    case Kaboom => killSomeAgentAtRandom()
    case _ => println("Supervisor - dont know how to handle it")
  }

  def initAgents(): Unit = {
    val system = ActorSystem(MainApp.AnimalsKnowledgeSystemName)

    val KnowledgeAgentAFS = system.actorOf(Props[KnowledgeAgentAFS], name = "KnowledgeAgentAFS")
    val KnowledgeAgentWikipedia = system.actorOf(Props[KnowledgeAgentWikipedia], name = "KnowledgeAgentWikipedia")
    val KnowledgeAgentWWF = system.actorOf(Props[KnowledgeAgentWWF], name = "KnowledgeAgentWWF")

    knowledgeAgentMap = KnowledgeAgentAFS :: knowledgeAgentMap
    knowledgeAgentMap = KnowledgeAgentWikipedia :: knowledgeAgentMap
    knowledgeAgentMap = KnowledgeAgentWWF :: knowledgeAgentMap

    for (elem <- knowledgeAgentMap) {
      elem ! FetchedAlreadyLearnedAnimals()
    }
  }

  def startLearning(animals: List[String]): Unit = {
    val system = ActorSystem(MainApp.AnimalsKnowledgeSystemName)

    val KnowledgeAgentAFS = system.actorOf(Props[KnowledgeAgentAFS], name = "KnowledgeAgentAFS")
    val KnowledgeAgentWikipedia = system.actorOf(Props[KnowledgeAgentWikipedia], name = "KnowledgeAgentWikipedia")
    val KnowledgeAgentWWF = system.actorOf(Props[KnowledgeAgentWWF], name = "KnowledgeAgentWWF")

    knowledgeAgentMap = KnowledgeAgentAFS :: knowledgeAgentMap
    knowledgeAgentMap = KnowledgeAgentWikipedia :: knowledgeAgentMap
    knowledgeAgentMap = KnowledgeAgentWWF :: knowledgeAgentMap

    for (elem <- animals) {
      KnowledgeAgentAFS ! LearnAbout(elem)
      KnowledgeAgentWikipedia ! LearnAbout(elem)
      KnowledgeAgentWWF ! LearnAbout(elem)
    }
  }

  def askAQuestion(userQuery: UsersQueryInstance): Unit = {
    for (elem <- knowledgeAgentMap) {
      elem ! userQuery
    }
  }

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _ =>
        // TODO - to chyba nie działa, bo wykorzystywany jest defaultowe strategy
        println("in supervisor strategy")
        Restart
    }
}

object KnowledgeAgentsSupervisor {

  val AgentName: String = "KnowledgeAgentsSupervisor"
  val FullAgentPath: String = "akka://" + MainApp.AnimalsKnowledgeSystemName +  "/user/" + AgentName

  final case class StartLearning(animals: List[String] = List("tiger", "koala", "spider", "parrot"))
  final case class InitAgents()
  final case class KillAgent(name: String)
}
