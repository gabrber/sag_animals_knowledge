package eiti.sag.knowledge_agents

import java.io.FileNotFoundException
import java.lang.Exception

import akka.pattern.ask
import akka.actor.ReceiveTimeout

import scala.concurrent.duration._
import eiti.sag.HttpServer.Kaboom
import eiti.sag.knowledge_agents.KnowledgeAgent.{FetchedAlreadyLearnedAnimals, LearnAbout}
import eiti.sag.query.{QueryType, UsersQueryInstance}

import scala.concurrent.Await

class KnowledgeAgentWWF extends KnowledgeAgent {

  val learned_animals = "wwf/learned_animals"
  val bag_of_words = "wwf/bag_of_words"
  val ner = "wwf/ner"
  val pos_ngrams = "wwf/pos_ngrams"
  val sentences = "wwf/sentences"
  val lemmaSentences = "wwf/lemma_sentences"
  val chunker = "wwf/chunker"
  val baseUrl = "https://www.worldwildlife.org/species/"

  def learn(animal : String): Unit ={
    println("WWF learning about " + animal)
    val animalUrl = baseUrl + animal

    learnAbout(animalUrl, animal, bag_of_words, ner, pos_ngrams, sentences, lemmaSentences, chunker)

    animalsLearnedAbout = animal :: animalsLearnedAbout
    persistAnimalsLearnedAbout(animalsLearnedAbout, learned_animals)
    println("WWF finished learning " + animal)
  }

  override def receive = {
    case Kaboom => kaboom()
    case FetchedAlreadyLearnedAnimals() => fetchAlreadLearnedAnimals(learned_animals)

    case LearnAbout(animal: String) =>
      learn(animal)
      context.setReceiveTimeout(1 minute)

    case usersQueryInstance: UsersQueryInstance =>

      if (!animalsLearnedAbout.contains(usersQueryInstance.animal)) {
        println("WWF - I don't know anything about this animal. Let me learn.")
        learn(usersQueryInstance.animal)
      }

      searchKnowledgeAndSendAnswer(usersQueryInstance, ner)
      try { val full_sent = findSentence(usersQueryInstance.mainWords, usersQueryInstance.animal, lemmaSentences, sentences)
      } catch { case _ => println("Cannot find sentence")}
      println("WWF is done")
      context.setReceiveTimeout(1 minute)

    case ReceiveTimeout ⇒
      println("Received timeout")

    case _      ⇒ log.info("received unknown message")
  }
}
