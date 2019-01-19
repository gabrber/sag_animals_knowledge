package eiti.sag


import akka.actor.{Actor, ActorSystem, PoisonPill}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import eiti.sag.knowledge_agents.KnowledgeAgentsSupervisor.KillAgent

import scala.io.StdIn
import scala.util.Random

class HttpServer extends Actor {
  implicit val actorSystem = context.system
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = actorSystem.dispatcher
  val random = new Random()

  val route =
    path("killagent") {
      get {
        val msg = "Send poison pill to agent: " + killAgentAtRandom()
        println(msg)
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>$msg</h1>"))
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => actorSystem.terminate()) // and shutdown when done

  def killAgentAtRandom(): String = {

    val agentsList = List("KnowledgeAgentWWF", "KnowledgeAgentAFS", "KnowledgeAgentWikipedia")

    val agentToKill: String =  agentsList(random.nextInt(agentsList.size))

    context.actorSelection("akka://AnimalsKnowledgeBase/user/KnowledgeAgentsSupervisor") ! KillAgent(agentToKill)

    agentToKill
  }

  override def receive: Receive = {
    case _ => println("Webserver received a msg")
  }
}
