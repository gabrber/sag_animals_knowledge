package eiti.sag.knowledge_agents

import java.net.URLEncoder

import akka.actor.PoisonPill
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
    case FetchedAlreadyLearnedAnimals() => fetchAlreadLearnedAnimals(learned_animals)
    case LearnAbout(animal: String) =>
      println("Wikipedia learning about " + animal)
      val animalUrl = baseUrl + URLEncoder.encode(animal.capitalize, "UTF-8")

      if (checkUrlExists(animalUrl)) {
        val pageContent = fetchContent(animalUrl)
        persistAsBagOfWords(pageContent, animal, bag_of_words)
        persistAsNERTokens(pageContent, animal, ner)
        persistAsPosNgrams(pageContent, animal, pos_ngrams)

        animalsLearnedAbout = animal :: animalsLearnedAbout
        persistAnimalsLearnedAbout(animalsLearnedAbout, learned_animals)
        println("Wikipedia has learned about " + animal)
      } else { log.info("Cannot find info about " + animal)}

    case usersQueryInstance: UsersQueryInstance =>
      searchKnowledgeAndSendAnswer(usersQueryInstance, ner)

    case _      ⇒ log.info("received unknown message")
  }
}
