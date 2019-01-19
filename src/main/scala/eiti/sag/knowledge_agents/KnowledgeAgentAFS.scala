package eiti.sag.knowledge_agents

import eiti.sag.knowledge_agents.KnowledgeAgent.{FetchedAlreadyLearnedAnimals, LearnAbout}
import eiti.sag.query.{QueryType, UsersQueryInstance}

class KnowledgeAgentAFS extends KnowledgeAgent {

  val learned_animals = "animal_facts_encyclopedia/learned_animals"
  val bag_of_words = "animal_facts_encyclopedia/bag_of_words"
  val ner = "animal_facts_encyclopedia/ner"
  val pos_ngrams = "animal_facts_encyclopedia/pos_ngrams"
  val sentences = "animal_facts_encyclopedia/sentences"
  val lemmaSentences = "animal_facts_encyclopedia/lemma_sentences"
  val chunker = "animal_facts_encyclopedia/chunker"
  val baseUrl = "https://www.animalfactsencyclopedia.com/"

  override def receive = {
    case FetchedAlreadyLearnedAnimals() => fetchAlreadLearnedAnimals(learned_animals)
    case LearnAbout(animal: String) =>
      println("AFS learning about " + animal)
      var animalUrl = ""
      if (animal.toLowerCase == "dog"){animalUrl = baseUrl + "All-About-Dogs.html"}
      else {animalUrl = baseUrl + animal.capitalize + "-facts.html"}

      println(animalUrl)
      if (checkUrlExists(animalUrl)) {
        val pageContent = fetchContent(animalUrl)
        persistAsBagOfWords(pageContent, animal, bag_of_words)
        persistAsNERTokens(pageContent, animal, ner)
        persistAsPosNgrams(pageContent, animal, pos_ngrams)

        animalsLearnedAbout = animal :: animalsLearnedAbout
        persistAnimalsLearnedAbout(animalsLearnedAbout, learned_animals)
        println("AFS has learned about " + animal)
      } else { log.info("Cannot find info about " + animal)}

    case usersQueryInstance: UsersQueryInstance =>
      searchKnowledgeAndSendAnswer(usersQueryInstance, ner)

    case _      ⇒ log.info("received unknown message")
  }
}
