package eiti.sag

import akka.actor.Actor
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, Node, TextNode}
import org.jsoup.select.NodeVisitor

class AnimalSpeciesNamesProvider extends Actor {

  var animalList: List[Any] = List()

  def doFetchAnimalNames(): Unit = {

    val url = "https://a-z-animals.com/animals/";
    val html = JsoupBrowser().get(url)
    val doc = Jsoup.parse(html.toString)

    doc.traverse(new MyNodeVisitor())
  }

  class MyNodeVisitor extends NodeVisitor {
    override def tail(node: Node, depth: Int): Unit = {}

    override def head(node: Node, depth: Int): Unit = {
      if(node.isInstanceOf[TextNode]) {

        val p = node.parent()
        val isok = p.asInstanceOf[Element].tagName().equals("b") &&
          p.parent().asInstanceOf[Element].tagName().equals("a") &&
          p.parent().parent().asInstanceOf[Element].tagName().equals("li") &&
          p.parent().parent().asInstanceOf[Element].attr("class").contains("az-phobia-link")

        if(isok) {
          animalList = node.asInstanceOf[TextNode].getWholeText :: animalList
        }
      }
    }
  }

  override def receive: Receive = {
    case "fetch" =>
      doFetchAnimalNames()
    case _ => println("Dont understand message")
  }
}
