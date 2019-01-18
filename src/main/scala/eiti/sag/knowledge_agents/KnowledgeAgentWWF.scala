package eiti.sag.knowledge_agents

import eiti.sag.knowledge_agents.KnowledgeAgent.{FetchedAlreadyLearnedAnimals, LearnAbout}
import eiti.sag.query.{QueryType, UsersQueryInstance}

class KnowledgeAgentWWF extends KnowledgeAgent {

  val learned_animals = "wwf/learned_animals"
  val bag_of_words = "wwf/bag_of_words"
  val ner = "wwf/ner"
  val pos_ngrams = "wwf/pos_ngrams"
  val baseUrl = "https://www.worldwildlife.org/species/"

  override def receive = {
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

        animalsLearnedAbout = animal :: animalsLearnedAbout
        persistAnimalsLearnedAbout(animalsLearnedAbout, learned_animals)
        println("WWF has learned about " + animal)
      } else { log.info("Cannot find info about " + animal)}

    case usersQueryInstance: UsersQueryInstance =>
      searchKnowledgeAndSendAnswer(usersQueryInstance, ner)

    case _      ⇒ log.info("received unknown message")
  }
}
