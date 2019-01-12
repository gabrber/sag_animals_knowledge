package eiti.sag

import org.jsoup.nodes.Document

//https://www.animalfactsencyclopedia.com
class KnowledgeAgentAFS extends KnowledgeAgent {

  //TODO
  override def readTable(doc:Document): String = {
    val table = doc.body.select("table").get(1)
    val rows = table.select("tr")
    var array: List[String] = List()
    var i = 0
    while (i < rows.size) { //first row is the col names so skip it.
      val row = rows.get(i)
      val cols = row.select("td")
      println(row)
      //println(cols)
      i += 1
    }
    var tableText = rows.text()
    return tableText
  }

  override def receive = {
    case "dog" =>
      var doc = readUrl("https://www.animalfactsencyclopedia.com/All-About-Dogs.html")
      println(readTable(doc))
    case animal ⇒ log.info("received message: " + animal.toString())
    case _      ⇒ log.info("received unknown message")
  }
}
