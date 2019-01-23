package eiti.sag

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, PoisonPill, Props, Terminated}
import akka.event.Logging
import eiti.sag.AnswerAgent.{AwaitForAnswer, ForceAnswerNow, FoundAnswer}
import eiti.sag.query.UsersQueryInstance

import scala.concurrent.duration._
import scala.language.postfixOps

class AnswerAgent extends Actor {
  val log = Logging(context.system, this)
  val KnowledgeAgentsNo = 3

  var queryToFoundAnswerList: scala.collection.mutable.Map[String, List[FoundAnswer]] = scala.collection.mutable.Map()

  def findBestAndSend(maybeAnswers: List[FoundAnswer]) = {
    if(maybeAnswers.isEmpty) {
      println("Sorry, cant answer")
    } else {
      val answer = maybeAnswers.sortBy(_.percentSure).head.answer
      println("Found answer: " + answer)
    }
  }

  def sendAnswerIfPossible(query: UsersQueryInstance): Unit = {
    if(queryToFoundAnswerList(query.originalQuery).size == KnowledgeAgentsNo) {
      findBestAndSend(queryToFoundAnswerList(query.originalQuery))
    }
  }

  def receive = {
    case ForceAnswerNow(q) =>
      findBestAndSend(queryToFoundAnswerList(q.originalQuery))
    case AwaitForAnswer(q) =>
      implicit val executionContext = context.system.dispatcher
      context.system.scheduler.scheduleOnce(7 second, self, ForceAnswerNow(q))
    case f: FoundAnswer =>
      val newAnswers = if(queryToFoundAnswerList.contains(f.query.originalQuery)) {
        f :: queryToFoundAnswerList(f.query.originalQuery)
      } else {
        List(f)
      }
      queryToFoundAnswerList.put(f.query.originalQuery, newAnswers)
      sendAnswerIfPossible(f.query)
    case _      ⇒ log.info("received unknown message")
  }
}

object AnswerAgent {

  final case class FoundAnswer(query: UsersQueryInstance, answer: String, percentSure: Float)

  final case class AwaitForAnswer(query: UsersQueryInstance)

  final case class ForceAnswerNow(query: UsersQueryInstance)

}