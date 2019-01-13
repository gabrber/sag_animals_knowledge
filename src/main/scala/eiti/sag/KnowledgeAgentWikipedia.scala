package eiti.sag

import java.io.{BufferedWriter, File, FileWriter}
import java.net.URLEncoder

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element, Node, TextNode}
import org.jsoup.select.NodeVisitor

class KnowledgeAgentWikipedia extends KnowledgeAgent {

  val HEURISTIC_CONTENT_LENGTH_THRESHOLD = 100

  def fetchContent(pageTitle: String): String = {
    val url = "https://en.wikipedia.org/wiki/" + URLEncoder.encode(pageTitle, "UTF-8")

    val html = JsoupBrowser().get(url)
    val doc = Jsoup.parse(html.toString)

    val sb = new StringBuilder()

    doc.traverse(new MyNodeVisitor(sb))

    sb.toString()
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

  override def receive = {
    case animal: String =>
      var pageContent = fetchContent(animal)
      println(pageContent)
      val wordsCounted = pageContent.split(" ")
        .map(p => p.trim.toLowerCase)
        .filter(p => p != "")
        .groupBy((word: String) => word)
        .mapValues(_.length)

      val file = new File("animal_db/wikipedia_bag_of_words/" + animal + ".txt")
      val bw = new BufferedWriter(new FileWriter(file))
      for (elem <- wordsCounted.keys) {
        bw.write(elem + " " + wordsCounted(elem) + "\n")
      }
    case _      ⇒ log.info("received unknown message")
  }
}
