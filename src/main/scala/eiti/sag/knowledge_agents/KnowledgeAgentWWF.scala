package eiti.sag.knowledge_agents

import java.io.FileNotFoundException
import java.lang.Exception

import eiti.sag.HttpServer.Kaboom
import eiti.sag.knowledge_agents.KnowledgeAgent.{FetchedAlreadyLearnedAnimals, LearnAbout}
import eiti.sag.query.{QueryType, UsersQueryInstance}

class KnowledgeAgentWWF extends KnowledgeAgent {

  val learned_animals = "wwf/learned_animals"
  val bag_of_words = "wwf/bag_of_words"
  val ner = "wwf/ner"
  val pos_ngrams = "wwf/pos_ngrams"
  val sentences = "wwf/sentences"
  val lemmaSentences = "wwf/lemma_sentences"
  val chunker = "wwf/chunker"
  val baseUrl = "https://www.worldwildlife.org/species/"

  override def receive = {
    case Kaboom => kaboom()
    case FetchedAlreadyLearnedAnimals() => fetchAlreadLearnedAnimals(learned_animals)
    case LearnAbout(animal: String) =>
      println("WWF learning about " + animal)
      val animalUrl = baseUrl + animal

      println(animalUrl)
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
        println("WWF has learned about " + animal)
      } else { log.info("Cannot find info about " + animal)}

    case usersQueryInstance: UsersQueryInstance =>
      searchKnowledgeAndSendAnswer(usersQueryInstance, ner)
      try { val full_sent = findSentence(usersQueryInstance.mainWords, usersQueryInstance.animal, lemmaSentences, sentences)
      } catch { case _ => println("Cannot find sentence")}
      println("WWF is done")
    case _      ⇒ log.info("received unknown message")
  }
}
