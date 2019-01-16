package eiti.sag
import eiti.sag.query.{QueryType, UsersQueryInstance}

class KnowledgeAgentAFS extends KnowledgeAgent {

  val bag_of_words = "afs_bag_of_words"
  val ner = "afs_ner"
  val baseUrl = "https://www.animalfactsencyclopedia.com/"

  override def receive = {
    case (animal:String, question:String) =>
      var animalUrl = ""
      if (animal.toLowerCase == "dog"){animalUrl = baseUrl + "All-About-Dogs.html"}
      else {animalUrl = baseUrl + animal.capitalize + "-facts.html"}

      println(animalUrl)
      if (checkUrlExists(animalUrl)) {
        val pageContent = fetchContent(animalUrl)
        persistAsBagOfWords(pageContent, animal, bag_of_words)
        persistAsNERTokens(pageContent, animal, ner)

        animalsLearnedAbout = animal :: animalsLearnedAbout

        // FIXME mocked :(
        self ! UsersQueryInstance(question, QueryType.Location)
      } else { log.info("Cannot find info about " + animal)}

    case usersQueryInstance: UsersQueryInstance =>
      searchKnowledgeAndSendAnswer(usersQueryInstance, ner)

    case _      ⇒ log.info("received unknown message")
  }
}
