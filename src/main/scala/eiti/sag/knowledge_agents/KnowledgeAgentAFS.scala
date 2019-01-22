package eiti.sag.knowledge_agents

import eiti.sag.HttpServer.Kaboom
import eiti.sag.knowledge_agents.KnowledgeAgent.{FetchedAlreadyLearnedAnimals, LearnAbout}
import eiti.sag.query.{QueryType, UsersQueryInstance}
import akka.actor.ReceiveTimeout
import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration._

class KnowledgeAgentAFS extends KnowledgeAgent {

  val learned_animals = "animal_facts_encyclopedia/learned_animals"
  val bag_of_words = "animal_facts_encyclopedia/bag_of_words"
  val ner = "animal_facts_encyclopedia/ner"
  val pos_ngrams = "animal_facts_encyclopedia/pos_ngrams"
  val sentences = "animal_facts_encyclopedia/sentences"
  val lemmaSentences = "animal_facts_encyclopedia/lemma_sentences"
  val chunker = "animal_facts_encyclopedia/chunker"
  val baseUrl = "https://www.animalfactsencyclopedia.com/"

  def learn(animal :String): Unit = {
    println("AFS learning about " + animal)
    var animalUrl = ""
    if (animal.toLowerCase == "dog"){animalUrl = baseUrl + "All-About-Dogs.html"}
    else {animalUrl = baseUrl + animal.capitalize + "-facts.html"}

    learnAbout(animalUrl, animal, bag_of_words, ner, pos_ngrams, sentences, lemmaSentences, chunker)

    animalsLearnedAbout = animal :: animalsLearnedAbout
    persistAnimalsLearnedAbout(animalsLearnedAbout, learned_animals)
    println("AFS finished learning about " + animal)
  }

  override def receive = {
    case Kaboom => kaboom()
    case FetchedAlreadyLearnedAnimals() => fetchAlreadLearnedAnimals(learned_animals)

    case LearnAbout(animal: String) =>
      learn(animal)
      context.setReceiveTimeout(1 minute)

    case usersQueryInstance: UsersQueryInstance =>
      var alreadyKnown :List[String] = fetchAlreadLearnedAnimals(learned_animals)

      if (!alreadyKnown.contains(usersQueryInstance.animal)) {
        println("AFS - I don't know anything about this animal. Let me learn.")
        learn(usersQueryInstance.animal)
      }

      searchKnowledgeAndSendAnswer(usersQueryInstance, ner)
      try{ val full_sent = findSentence(usersQueryInstance.mainWords,usersQueryInstance.animal,lemmaSentences,sentences)
      } catch { case _ => println("Cannot find sentence")}
      println("AFS is done")
      context.setReceiveTimeout(1 minute)

    case ReceiveTimeout ⇒
      println("Received timeout")

    case _      ⇒ log.info("received unknown message")
  }
}
