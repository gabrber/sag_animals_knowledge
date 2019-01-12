package eiti.sag

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, PoisonPill, Props, Terminated}
import akka.event.Logging

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Await

//https://github.com/ruippeixotog/scala-scraper
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model._

//JSOUP
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

class KnowledgeAgent extends Actor {
  val log = Logging(context.system, this)

  //Receive doc from URL
  def readUrl(textUrl: String): Document = {
    val browser = JsoupBrowser()
    val html = browser.get(textUrl)
    html >> allText
    val doc = Jsoup.parse(html.toString)

    return doc
  }

  //read body from html doc
  def readAllBody(doc:Document): String = {
    val docBody = doc.body.text

    return docBody
  }

  //read parapgrapgs form html doc
  def readParagraphs(doc: Document): String = {
    val paragraphs = doc.body.select("p")
    val pText = paragraphs.text
    return pText
  }

  //read table
  def readTable(doc:Document): String = {
    val table = doc.body.select("table")
    val rows = table.select("tr")

//    var i=0
//    while (i < rows.size) {
//      val row = rows.get(i)
//      val cols = row.select("td")
//      i += 1
//    }
//
    var tableText = rows.text()
    return tableText
  }

  // Receive Message cases
  def receive = {
    case animal ⇒ log.info("received message: " + animal.toString())
    case _      ⇒ log.info("received unknown message")
  }
}
