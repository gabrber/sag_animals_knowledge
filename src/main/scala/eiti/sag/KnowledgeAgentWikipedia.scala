package eiti.sag

import java.io._
import java.net.URLEncoder

import eiti.sag.query.{QueryType, UsersQueryInstance}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import opennlp.tools.namefind.{NameFinderME, TokenNameFinderModel}
import opennlp.tools.tokenize.{TokenizerME, TokenizerModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element, Node, TextNode}
import org.jsoup.select.NodeVisitor

import scala.io.Source

class KnowledgeAgentWikipedia extends KnowledgeAgent {

  val bag_of_words = "wikipedia/bag_of_words"
  val ner = "wikipedia/ner"
  val pos_ngrams = "wikipedia/pos_ngrams"
  val baseUrl = "https://en.wikipedia.org/wiki/"

  override def receive = {
    case (animal:String, question:String, questionType: QueryType.Value) =>
      var animalUrl = baseUrl + URLEncoder.encode(animal.capitalize, "UTF-8")

      println(animalUrl)
      if (checkUrlExists(animalUrl)) {
        val pageContent = fetchContent(animalUrl)
        persistAsBagOfWords(pageContent, animal, bag_of_words)
        persistAsNERTokens(pageContent, animal, ner)
        persistAsPosNgrams(pageContent, animal, pos_ngrams)

        animalsLearnedAbout = animal :: animalsLearnedAbout

        // FIXME mocked :(
        self ! UsersQueryInstance(question + " " + animal, questionType)
      } else { log.info("Cannot find info about " + animal)}

    case usersQueryInstance: UsersQueryInstance =>
      searchKnowledgeAndSendAnswer(usersQueryInstance, ner)

    case _      ⇒ log.info("received unknown message")
  }
}
