package eiti.sag
import eiti.sag.query.{QueryType, UsersQueryInstance}

class KnowledgeAgentAFS extends KnowledgeAgent {

  val bag_of_words = "animal_facts_encyclopedia/bag_of_words"
  val ner = "animal_facts_encyclopedia/ner"
  val pos_ngrams = "animal_facts_encyclopedia/pos_ngrams"
  val baseUrl = "https://www.animalfactsencyclopedia.com/"

  override def receive = {
    case (animal:String, question:String, questionType: QueryType.Value) =>
      var animalUrl = ""
      if (animal.toLowerCase == "dog"){animalUrl = baseUrl + "All-About-Dogs.html"}
      else {animalUrl = baseUrl + animal.capitalize + "-facts.html"}

      println(animalUrl)
      if (checkUrlExists(animalUrl)) {
        val pageContent = fetchContent(animalUrl)
        persistAsBagOfWords(pageContent, animal, bag_of_words)
        persistAsNERTokens(pageContent, animal, ner)
        persistAsPosNgrams(pageContent, animal, pos_ngrams)

        animalsLearnedAbout = animal :: animalsLearnedAbout

        self ! UsersQueryInstance(question + " " + animal, questionType)
      } else { log.info("Cannot find info about " + animal)}

    case usersQueryInstance: UsersQueryInstance =>
      searchKnowledgeAndSendAnswer(usersQueryInstance, ner)

    case _      ⇒ log.info("received unknown message")
  }
}
