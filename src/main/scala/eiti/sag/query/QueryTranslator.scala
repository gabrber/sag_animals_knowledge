package eiti.sag.query

import akka.actor.{Actor, ActorSystem, Props}
import eiti.sag.KnowledgeAgentWikipedia

class QueryTranslator extends Actor {

  // FIXME - do rozbudowania ręcznego
  // należy także dołożyć mechanizmy, które budują automatycznie
  val keywordListToQueryTypeMap = Map(
    List("where") -> QueryType.Location,
    List("color", "colour") -> QueryType.Color,
    List("food", "eat", "eats") -> QueryType.Feeding,
    List("species") -> QueryType.Classification,
    List("weight") -> QueryType.Weight)



  override def receive: Receive = {
    case usersRawQuery: String =>
      parseAndResend(usersRawQuery)
    case _ => println("Cant understand")
  }


  def sendQueryToAllAnswerAgents(q: UsersQueryInstance): Unit = {

    // FIXME - send to all, not just wiki
    val system = ActorSystem("AnimalsKnowledgeBase")
    val wikiActor = system.actorOf(Props[KnowledgeAgentWikipedia])

    wikiActor ! q
  }

  def parseAndResend(usersRawQuery: String): Unit = {
    val usersRawQueryLowerCase = usersRawQuery.toLowerCase()

    val queryType = determineType(usersRawQueryLowerCase)

    if(queryType == null) {
      // FIXME - notify by sending message, not just printing
      println("Sorry, cannot find answer to query: " + usersRawQuery)
    } else {

      val q = UsersQueryInstance(usersRawQuery, queryType)
      sendQueryToAllAnswerAgents(q)

    }
  }

  def determineType(str: String): QueryType.Value = {
    for (keywords <- keywordListToQueryTypeMap.keys) {
      for (k <- keywords) {
        if(str.contains(k)) {
          return keywordListToQueryTypeMap(keywords)
        }
      }
    }
    null
  }
}
