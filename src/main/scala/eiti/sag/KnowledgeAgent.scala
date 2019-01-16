package eiti.sag

import java.io._

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, PoisonPill, Props, Terminated}
import akka.event.Logging
import eiti.sag.query.{QueryType, UsersQueryInstance}
import opennlp.tools.namefind.{NameFinderME, TokenNameFinderModel}
import opennlp.tools.tokenize.{TokenizerME, TokenizerModel}
import org.jsoup.nodes.{Node, TextNode}
import org.jsoup.select.NodeVisitor

import scala.language.postfixOps
import scala.io.Source
import scalaj.http._

//https://github.com/ruippeixotog/scala-scraper
import net.ruippeixotog.scalascraper.browser.JsoupBrowser

//JSOUP
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

class KnowledgeAgent extends Actor {
  val log = Logging(context.system, this)

  val knowledgeBaseSep = ";"

  val locationModelFile = "database/en-ner-location.bin"
  val tokenModelFile = "database/en-token.bin"
  val HEURISTIC_CONTENT_LENGTH_THRESHOLD = 100

  // FIXME - to nie jest fault tolerant
  var animalsLearnedAbout: List[String] = List()

  def checkUrlExists(checkUrl: String): Boolean = {
    val response = Http(checkUrl).asString.code
    var ifExists = false
    if (response != 404) { ifExists = true}
    ifExists
  }

  def fetchContent(pageTitle: String): String = {
    val url = pageTitle
    val html = JsoupBrowser().get(url)
    val doc = Jsoup.parse(html.toString)
    val sb = new StringBuilder()
    doc.traverse(new MyNodeVisitor(sb))
    sb.toString()
  }

  def persistAsBagOfWords(pageContent: String, animal: String, dirname: String) = {
    val wordsCounted = pageContent.split(" ")
      .map(p => p.trim.toLowerCase)
      .filter(p => p != "")
      .groupBy((word: String) => word)
      .mapValues(_.length)

    // TODO - encode filename
    val file = new File("animal_db/" + dirname + "/" + animal + ".txt")
    val bw = new BufferedWriter(new FileWriter(file))
    for (elem <- wordsCounted.keys) {
      bw.write(elem + knowledgeBaseSep + wordsCounted(elem) + "\n")
    }
  }

  def persistAsNERTokens(pageContent: String, animal: String, dirname: String): Unit = {

    println("Loading model ...")

    val bis = new BufferedInputStream(new FileInputStream(locationModelFile))
    val model = new TokenNameFinderModel(bis)
    val nameFinder = new NameFinderME(model)

    println("Model loaded. Tokenizing ...")
    val tokens = tokenize(pageContent)
    val nameSpans = nameFinder.find(tokens)

    val locationToCertaintyMap =
      for(ns <- nameSpans) yield {
        val substring: String = tokens.slice(ns.getStart, ns.getEnd).mkString(" ")
        val prob: Double = ns.getProb
        val entityType = ns.getType
        (substring, prob)
      }

    val locationToWeightedCertaintyMap = locationToCertaintyMap
      .groupBy(_._1)
      .mapValues(_.map(_._2).sum)

    val file = new File("animal_db/" + dirname + "/" + animal + ".txt")
    val bw = new BufferedWriter(new FileWriter(file))
    for (elem <- locationToWeightedCertaintyMap.keys) {
      bw.write(elem + knowledgeBaseSep + locationToWeightedCertaintyMap(elem) + "\n")
    }
    bw.close()
  }

  def searchKnowledgeAndSendAnswer(usersQueryInstance: UsersQueryInstance, dirname: String): Unit = {
    findLocationUsingNERTags(usersQueryInstance, dirname)
  }

  def findLocationUsingNERTags(usersQueryInstance: UsersQueryInstance, dirname: String): Unit = {

    def extractAnimal(query: String): String = {
      for (elem <- animalsLearnedAbout) {
        if(query.contains(elem)) {
          return elem
        }
      }
      null
    }

    val animal = extractAnimal(usersQueryInstance.originalQuery)
    if(animal == null) {
      println("Query: " + usersQueryInstance + ". Didnot find answer :(")
      return
    }

    // FIXME mocked :(
    // możnaby trzymać gdzieś listę zwierząt, o których się nauczyliśmy, i przeszukiwać pytanie pod tym kątem
    val file = new File("animal_db/" + dirname + "/" + animal + ".txt")
    val content = Source.fromFile("animal_db/" + dirname + "/" + animal + ".txt").mkString
    val mostCertainLocation = content.split("\n").filter(line => !line.isEmpty).map(line => {
      val word = line.split(knowledgeBaseSep)(0)
      val certainty = line.split(knowledgeBaseSep)(1).toDouble
      (word, certainty)
    }).maxBy(_._2)._1

    println("Query: " + usersQueryInstance.originalQuery + ". Found answer: " + mostCertainLocation)
  }

  @throws[IOException]
  def tokenize(sentence: String): Array[String] = {
    val bis = new BufferedInputStream(new FileInputStream(tokenModelFile))
    val tokenModel = new TokenizerModel(bis)
    val tokenizer = new TokenizerME(tokenModel)
    tokenizer.tokenize(sentence)
  }

  class MyNodeVisitor(stringBuilder: StringBuilder) extends NodeVisitor {
    override def tail(node: Node, depth: Int): Unit = {}

    override def head(node: Node, depth: Int): Unit = {

      val legalTags = List("p", "a", "b")

      if(node.isInstanceOf[TextNode]) {
        val p = node.asInstanceOf[TextNode].parent()
        if(p.isInstanceOf[Element]) {
          if(legalTags.contains(p.asInstanceOf[Element].tagName())) {
            val nodeText = node.asInstanceOf[TextNode].getWholeText
            if(nodeText.length > HEURISTIC_CONTENT_LENGTH_THRESHOLD) {
              // heurystyka: jeśli rozmiar elementu jest wystarczająco duży
              // możemy założyć, że jest to treść
              stringBuilder.append(nodeText)
            }
          }
        }
      }
    }
  }

  // Receive Message cases
  def receive = {
    case _      ⇒ log.info("received unknown message")
  }
}
