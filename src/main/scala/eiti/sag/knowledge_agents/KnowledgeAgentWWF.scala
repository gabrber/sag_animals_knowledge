package eiti.sag.knowledge_agents

import java.io.{BufferedWriter, File, FileNotFoundException, FileWriter}
import java.lang.Exception

import akka.actor.ReceiveTimeout

import scala.concurrent.duration._
import eiti.sag.HttpServer.Kaboom
import eiti.sag.knowledge_agents.KnowledgeAgent.{FetchedAlreadyLearnedAnimals, LearnAbout}
import eiti.sag.query.{QueryType, UsersQueryInstance}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.jsoup.Jsoup

class KnowledgeAgentWWF extends KnowledgeAgent {

  val learned_animals = "wwf/learned_animals"
  val bag_of_words = "wwf/bag_of_words"
  val ner = "wwf/ner"
  val pos_ngrams = "wwf/pos_ngrams"
  val sentences = "wwf/sentences"
  val lemmaSentences = "wwf/lemma_sentences"
  val chunker = "wwf/chunker"
  val tables = "wwf/tables"
  val baseUrl = "https://www.worldwildlife.org/species/"

  override def receive = {
    case Kaboom => kaboom()
    case FetchedAlreadyLearnedAnimals() => fetchAlreadLearnedAnimals(learned_animals)

    case LearnAbout(animal: String) =>
      println("WWF learning about " + animal)
      val animalUrl = baseUrl + animal

      println(animalUrl)
      if (checkUrlExists(animalUrl)) {
        getTables(animalUrl,animal)
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
      context.setReceiveTimeout(1 minute)

    case usersQueryInstance: UsersQueryInstance =>
      searchKnowledgeAndSendAnswer(usersQueryInstance, ner)
      try { val full_sent = findSentence(usersQueryInstance.mainWords, usersQueryInstance.animal, lemmaSentences, sentences)
      } catch { case _ => println("Cannot find sentence")}
      println("WWF is done")
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

    var tableText=doc.select("div [class=container]")
    var tableText2=doc.select("[class=hdr]")
    try{
      for(i <- 0 to 12)
      {
        if(tableText2.get(i).text()=="Status")bw.write("Status;"+tableText.get(i).text()+"\n")
        if(tableText2.get(i).text()=="Population")bw.write("Population;"+tableText.get(i).text()+"\n")
        if(tableText2.get(i).text()=="Scientific Name")bw.write("Scientific Name;"+tableText.get(i).text()+"\n")
        if(tableText2.get(i).text()=="Weight")bw.write("Weight;"+tableText.get(i).text()+"\n")
        if(tableText2.get(i).text()=="Length")bw.write("Length;"+tableText.get(i).text()+"\n")
        if(tableText2.get(i).text()=="Habitats")bw.write("Habitats;"+tableText.get(i).text()+"\n")
        if(tableText2.get(i).text()=="Height")bw.write("Height;"+tableText.get(i).text()+"\n")
      }
    }catch{
      case e: IndexOutOfBoundsException => {bw.write("brak wyniku")
        bw.close()
        return "brak wyniku"}
    }
    bw.close()

  }
}
