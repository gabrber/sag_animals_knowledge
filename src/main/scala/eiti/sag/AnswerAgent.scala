package eiti.sag

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, PoisonPill, Props, Terminated}
import akka.event.Logging
import eiti.sag.AnswerAgent.{AwaitForAnswer, ForceAnswerNow, FoundAnswer}
import eiti.sag.query.UsersQueryInstance

import scala.concurrent.duration._
import scala.language.postfixOps

class AnswerAgent extends Actor {
  val log = Logging(context.system, this)

  var queryToFoundAnswerList: scala.collection.mutable.Map[UsersQueryInstance, List[FoundAnswer]] = scala.collection.mutable.Map()

  def findBestAndSend(maybeAnswers: List[FoundAnswer]) = {
    if(maybeAnswers.isEmpty) {
      println("Sorry, cant answer")
    } else {
      val answer = maybeAnswers.sortBy(_.percentSure).head.answer
      println(answer)
    }
  }

  def sendAnswerIfPossible(query: UsersQueryInstance): Unit = {
    if(queryToFoundAnswerList.get(query).size == 3) {
      findBestAndSend(queryToFoundAnswerList.get(query).getOrElse(List()))
    }
  }

  def receive = {
    case ForceAnswerNow(q) =>
      findBestAndSend(queryToFoundAnswerList(q))
    case AwaitForAnswer(q) =>
      implicit val executionContext = context.system.dispatcher
      context.system.scheduler.scheduleOnce(10 second, self, ForceAnswerNow(q))
    case f: FoundAnswer =>
      queryToFoundAnswerList.put(f.query, f :: queryToFoundAnswerList.get(f.query).getOrElse(List()))
      sendAnswerIfPossible(f.query)
    case _      ⇒ log.info("received unknown message")
  }
}

object AnswerAgent {

  final case class FoundAnswer(query: UsersQueryInstance, answer: String, percentSure: Float)

  final case class AwaitForAnswer(query: UsersQueryInstance)

  final case class ForceAnswerNow(query: UsersQueryInstance)

}