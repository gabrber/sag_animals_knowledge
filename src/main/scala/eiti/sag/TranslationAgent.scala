package eiti.sag

import java.io.{BufferedInputStream, FileInputStream}

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, PoisonPill, Props, Terminated}
import akka.event.Logging
import eiti.sag.TranslationAgent.{SingleWordInSentence, TaggedQuery}
import eiti.sag.query.KnownPosTags.KnownPosTags

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Await
import eiti.sag.query._
import opennlp.tools.postag.{POSModel, POSTaggerME}
import opennlp.tools.tokenize.WhitespaceTokenizer

class TranslationAgent extends Actor {
  val log = Logging(context.system, this)

  val lexiconFileName = "database/en-pos-maxent.bin"

  val model = new POSModel(new BufferedInputStream(new FileInputStream(lexiconFileName)))
  val tagger = new POSTaggerME(model)


  // Get animal name from user
  def greetings(): String = {
    println("Hello! Which animal are you interested in?")
    var animal = scala.io.StdIn.readLine()
    println("Okay. Looking for information about " + animal)
    return animal
  }

  def getQuestion(animal:String): String = {
    println("What do you want to know about " + animal + "?")
    var infoType = scala.io.StdIn.readLine()
    println("Okay. Looking for " + infoType)
    return infoType
  }

  def getQuestionType(question: String): QueryType.Value = {

    for (word <- question.split(" +")){
      for ((k,v) <- QueryMap.keywordListToQueryTypeMap) if(k.contains(word)) return v
    }
    return null
  }

  def tag(text: String) = {
    // znaczenie tag'ów: http://paula.petcu.tm.ro/init/default/post/opennlp-part-of-speech-tags
    // nie dzielimy na zdania, bo zakładamy, że zapytanie użytkownika jest już pojedynczynym zdaniem
    val whitespaceTokenizerLine: Array[String] = WhitespaceTokenizer.INSTANCE.tokenize(text)
    val tags: Array[String] = tagger.tag(whitespaceTokenizerLine)
    val tokenizedSentence = (tags zip whitespaceTokenizerLine).zipWithIndex map {
      case ((tag: String, word: String), index: Int) => SingleWordInSentence(word, tag, KnownPosTags.fromString(tag), index)
    }

    TaggedQuery(tokenizedSentence)
  }

  // Choose one Agent with name matching pattern
  def choseOneAgent(patternName:String): ActorRef = {
    var test = context.actorSelection("akka://AnimalsKnowledgeBase/user/" + patternName + "*").resolveOne(5 second)
    var agent = Await.result(test,5 second)
    return agent
  }

  // Choose all Agents with name matching pattern
  def choseAllAgents(patternName:String): ActorSelection = {
    var agent = context.actorSelection("akka://AnimalsKnowledgeBase/user/" + patternName + "*")
    return agent
  }



  // Receive Message cases
  def receive = {
    case "greetings" ⇒
      val animal = greetings().toLowerCase
      val question = getQuestion(animal)
      val questionType = getQuestionType(question)
      val tagged = tag(question)

      if(questionType == null){
        log.info("Cannot resolve question type")
      }

      context.actorSelection("akka://AnimalsKnowledgeBase/user/KnowledgeAgentsSupervisor") ! UsersQueryInstance(animal, questionType, tagged)

    case _      ⇒ log.info("received unknown message")
  }
}

object TranslationAgent {
  case class SingleWordInSentence(word: String, posRaw: String, pos: KnownPosTags, index: Int)

  case class TaggedQuery(sentence: Array[SingleWordInSentence]) {
    assert(sentence.map(word => word.index).sorted sameElements  sentence.map(w => w.index))

  }
}