package eiti.sag.knowledge_agents

import java.net.URLEncoder

import akka.actor.{PoisonPill, ReceiveTimeout}

import scala.concurrent.duration._
import eiti.sag.HttpServer.Kaboom
import eiti.sag.knowledge_agents.KnowledgeAgent.{FetchedAlreadyLearnedAnimals, LearnAbout}
import eiti.sag.knowledge_agents.KnowledgeAgentsSupervisor.StartLearning
import eiti.sag.query.{QueryType, UsersQueryInstance}

class KnowledgeAgentWikipedia extends KnowledgeAgent {

  val learned_animalsFile = "wikipedia/learned_animals"
  val bag_of_wordsFile = "wikipedia/bag_of_words"
  val nerFile = "wikipedia/ner"
  val pos_ngramsFile = "wikipedia/pos_ngrams"
  val sentencesFile = "wikipedia/sentences"
  val lemmaSentencesFile = "wikipedia/lemma_sentences"
  val chunkerFile = "wikipedia/chunker"
  val baseUrl = "https://en.wikipedia.org/wiki/"

  def learn(animal :String): Unit = {
    println("Wikipedia learning about " + animal)
    val animalUrl = baseUrl + URLEncoder.encode(animal.capitalize, "UTF-8")
    if (checkUrlExists(animalUrl)) {
      learnAbout(animalUrl, animal, bag_of_wordsFile, nerFile, pos_ngramsFile, sentencesFile, lemmaSentencesFile, chunkerFile)
    }  else { log.info("Cannot find info about " + animal)}
    animalsLearnedAbout = animal :: animalsLearnedAbout
    persistAnimalsLearnedAbout(animalsLearnedAbout, learned_animalsFile)
    println("Wikipedia finished learning about " + animal)
  }

  override def receive = {
    case Kaboom => kaboom()
    case FetchedAlreadyLearnedAnimals() => fetchAlreadLearnedAnimals(learned_animalsFile)
    case LearnAbout(animal: String) =>
      learn(animal)
      context.setReceiveTimeout(1 minute)
      askForAnimalToLearnAbout()

    case usersQueryInstance: UsersQueryInstance =>
      if (!animalsLearnedAbout.contains(usersQueryInstance.animal)) {
        println("Wikipedia - I don't know anything about this animal. Let me learn.")
        learn(usersQueryInstance.animal)
      }

      searchKnowledgeAndSendAnswer(usersQueryInstance, nerFile)
      try{ val full_sent = findSentence(usersQueryInstance.mainWords,usersQueryInstance.animal,lemmaSentencesFile,sentencesFile)
      } catch { case _ => println("Cannot find sentence")}
      println("Wikipedia is done")
      context.setReceiveTimeout(1 minute)

    case ReceiveTimeout ⇒
      println("Received timeout")

    case _      ⇒ log.info("received unknown message")
  }
}
