package eiti.sag.knowledge_agents

import java.net.URLEncoder

import akka.actor.{PoisonPill, ReceiveTimeout}
import akka.pattern.ask

import scala.concurrent.duration._
import eiti.sag.HttpServer.Kaboom
import eiti.sag.knowledge_agents.KnowledgeAgent.{FetchedAlreadyLearnedAnimals, LearnAbout}
import eiti.sag.query.{QueryType, UsersQueryInstance}

import scala.concurrent.Await

class KnowledgeAgentWikipedia extends KnowledgeAgent {

  val learned_animals = "wikipedia/learned_animals"
  val bag_of_words = "wikipedia/bag_of_words"
  val ner = "wikipedia/ner"
  val pos_ngrams = "wikipedia/pos_ngrams"
  val sentences = "wikipedia/sentences"
  val lemmaSentences = "wikipedia/lemma_sentences"
  val chunker = "wikipedia/chunker"
  val baseUrl = "https://en.wikipedia.org/wiki/"

  def learn(animal :String): Unit = {
    println("Wikipedia learning about " + animal)
    val animalUrl = baseUrl + URLEncoder.encode(animal.capitalize, "UTF-8")
    if (checkUrlExists(animalUrl)) {
      learnAbout(animalUrl, animal, bag_of_words, ner, pos_ngrams, sentences, lemmaSentences, chunker)
    }  else { log.info("Cannot find info about " + animal)}
    animalsLearnedAbout = animal :: animalsLearnedAbout
    persistAnimalsLearnedAbout(animalsLearnedAbout, learned_animals)
    println("Wikipedia finished learning about " + animal)
  }

  override def receive = {
    case Kaboom => kaboom()
    case FetchedAlreadyLearnedAnimals() => fetchAlreadLearnedAnimals(learned_animals)

    case LearnAbout(animal: String) =>
      learn(animal)
      context.setReceiveTimeout(1 minute)

    case usersQueryInstance: UsersQueryInstance =>
      if (!animalsLearnedAbout.contains(usersQueryInstance.animal)) {
        println("Wikipedia - I don't know anything about this animal. Let me learn.")
        learn(usersQueryInstance.animal)
      }

      searchKnowledgeAndSendAnswer(usersQueryInstance, ner)
      try{ val full_sent = findSentence(usersQueryInstance.mainWords,usersQueryInstance.animal,lemmaSentences,sentences)
      } catch { case _ => println("Cannot find sentence")}
      println("Wikipedia is done")
      context.setReceiveTimeout(1 minute)

    case ReceiveTimeout ⇒
      println("Received timeout")

    case _      ⇒ log.info("received unknown message")
  }
}
