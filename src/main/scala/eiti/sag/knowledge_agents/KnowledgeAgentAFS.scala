package eiti.sag.knowledge_agents

import java.io.{BufferedWriter, File, FileWriter}

import eiti.sag.HttpServer.Kaboom
import eiti.sag.knowledge_agents.KnowledgeAgent.{FetchedAlreadyLearnedAnimals, LearnAbout}
import eiti.sag.query.{QueryType, UsersQueryInstance}
import akka.actor.ReceiveTimeout
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.jsoup.Jsoup
import scala.concurrent.Await
import eiti.sag.meta_knowledge_agents.MetaKnowledgeAgentsSupervisor.AskForAnimalSpecies

import scala.concurrent.duration._


class KnowledgeAgentAFS extends KnowledgeAgent {

  val learned_animals = "animal_facts_encyclopedia/learned_animals"
  val bag_of_words = "animal_facts_encyclopedia/bag_of_words"
  val ner = "animal_facts_encyclopedia/ner"
  val pos_ngrams = "animal_facts_encyclopedia/pos_ngrams"
  val sentences = "animal_facts_encyclopedia/sentences"
  val lemmaSentences = "animal_facts_encyclopedia/lemma_sentences"
  val chunker = "animal_facts_encyclopedia/chunker"
  val tables = "animal_facts_encyclopedia/tables"
  val baseUrl = "https://www.animalfactsencyclopedia.com/"

  def learn(animal :String): Unit = {
    println("AFS learning about " + animal)
    var animalUrl = ""
    if (animal.toLowerCase == "dog"){animalUrl = baseUrl + "All-About-Dogs.html"}
    else {animalUrl = baseUrl + animal.capitalize + "-facts.html"}

    println(animalUrl)
    if (checkUrlExists(animalUrl)) {

      getTables(animalUrl, animal)
      learnAbout(animalUrl, animal, bag_of_words, ner, pos_ngrams, sentences, lemmaSentences, chunker)

      animalsLearnedAbout = animal :: animalsLearnedAbout
      persistAnimalsLearnedAbout(animalsLearnedAbout, learned_animals)
      println("AFS finished learning about " + animal)
    } else { log.info("Cannot find info about " + animal)}
  }

  override def receive = {
    case Kaboom => kaboom()
    case FetchedAlreadyLearnedAnimals() => fetchAlreadLearnedAnimals(learned_animals)
    case LearnAbout(animal: String) =>
      learn(animal)
      animalsLearnedAbout = animal :: animalsLearnedAbout
      context.setReceiveTimeout(1 minute)
      askForAnimalToLearnAbout()

    case usersQueryInstance: UsersQueryInstance =>
      if (!animalsLearnedAbout.contains(usersQueryInstance.animal)) {
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

  def getTables(pageTitle: String, animal:String): Unit = {
    val file = new File("animal_db/" + tables + "/" + animal + ".txt")
    val bw = new BufferedWriter(new FileWriter(file))
    val url = pageTitle
    val html = JsoupBrowser().get(url)
    val doc = Jsoup.parse(html.toString)

    var table = doc.select("table[style*=text-align]")
    val rows = table.select("tr")

    var z = new Array[String](80)
    for (i <- 0 to 79)
      z(i)=""

    try{
      for(i <- 1 to rows.size-1)
      {
        val row = rows.get(i)
        for(j <- 0 to 3)
        {
          val cells = row.select("td")
          if(j<cells.size){
            val cell = cells.get(j).text()
            z((i-1)*4+j) = cell
          }
        }
      }

      for(l<-0 to 5){
        for(k<-0 to 3){
          bw.write(z(l*8+k) + knowledgeBaseSep + z(l*8+4+k) + "\n")
        }
      }
    }catch{
      case e: IndexOutOfBoundsException => {bw.close()
        return "brak wyniku"}
    }
    bw.close()
  }
}
