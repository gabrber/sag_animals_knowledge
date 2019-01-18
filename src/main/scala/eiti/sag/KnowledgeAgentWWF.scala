package eiti.sag

import eiti.sag.query.{QueryType, UsersQueryInstance}

class KnowledgeAgentWWF extends KnowledgeAgent {

  val bag_of_words = "wwf/bag_of_words"
  val ner = "wwf/ner"
  val pos_ngrams = "wwf/pos_ngrams"
  val baseUrl = "https://www.worldwildlife.org/species/"

  override def receive = {
    case (animal:String, question:String, questionType: QueryType.Value) =>
      var animalUrl = baseUrl + animal

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
