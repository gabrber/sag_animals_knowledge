package eiti.sag.knowledge_agents

import java.net.URLEncoder

import akka.actor.{PoisonPill, ReceiveTimeout}
import scala.concurrent.duration._
import eiti.sag.HttpServer.Kaboom
import eiti.sag.knowledge_agents.KnowledgeAgent.{FetchedAlreadyLearnedAnimals, LearnAbout}
import eiti.sag.query.{QueryType, UsersQueryInstance}

class KnowledgeAgentWikipedia extends KnowledgeAgent {

  val learned_animals = "wikipedia/learned_animals"
  val bag_of_words = "wikipedia/bag_of_words"
  val ner = "wikipedia/ner"
  val pos_ngrams = "wikipedia/pos_ngrams"
  val sentences = "wikipedia/sentences"
  val lemmaSentences = "wikipedia/lemma_sentences"
  val chunker = "wikipedia/chunker"
  val baseUrl = "https://en.wikipedia.org/wiki/"

  override def receive = {
    case Kaboom => kaboom()
    case FetchedAlreadyLearnedAnimals() => fetchAlreadLearnedAnimals(learned_animals)

    case LearnAbout(animal: String) =>
      println("Wikipedia learning about " + animal)
      val animalUrl = baseUrl + URLEncoder.encode(animal.capitalize, "UTF-8")

      if (checkUrlExists(animalUrl)) {
        val pageContent = fetchContent(animalUrl)
        persistAsBagOfWords(pageContent, animal, bag_of_words)
        persistAsNERTokens(pageContent, animal, ner)
        persistAsPosNgrams(pageContent, animal, pos_ngrams)
        persistAsSentences(pageContent, animal, sentences)
        persistAsLemmaSentences(sentences, animal, lemmaSentences)
        persistAsChunker(pageContent, animal, chunker)

        animalsLearnedAbout = animal :: animalsLearnedAbout
        persistAnimalsLearnedAbout(animalsLearnedAbout, learned_animals)
        println("Wikipedia has learned about " + animal)
      } else { log.info("Cannot find info about " + animal)}
      context.setReceiveTimeout(1 minute)

    case usersQueryInstance: UsersQueryInstance =>
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
