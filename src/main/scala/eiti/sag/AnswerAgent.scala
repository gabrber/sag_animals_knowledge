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
  var alreadyAnswered: List[UsersQueryInstance] = List()

  def findBestAndSend(query: UsersQueryInstance, answers: List[FoundAnswer]): Unit = {
    if(alreadyAnswered.contains(query)) {
      return
    } else {
      alreadyAnswered = query :: alreadyAnswered
    }
    if(answers.isEmpty) {
      println("Sorry, cant answer")
    } else {
      val answer = answers.sortBy(_.percentSure).last.answer
      println("Found answer: " + answer)
      context.actorSelection("akka://AnimalsKnowsledgeBase/user/SystemUserAgent1") ! "mainMenu"
    }
  }

  def sendAnswerIfPossible(query: UsersQueryInstance): Unit = {
    if(queryToFoundAnswerList(query.originalQuery).size == KnowledgeAgentsNo) {
      findBestAndSend(query, queryToFoundAnswerList(query.originalQuery))
    }
  }

  def receive = {
    case ForceAnswerNow(q) =>
      findBestAndSend(q, queryToFoundAnswerList(q.originalQuery))
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