package eiti.sag

import java.io.{BufferedInputStream, FileInputStream}

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Identify, Kill, PoisonPill, Props, ReceiveTimeout, Terminated}
import akka.event.Logging
import eiti.sag.TranslationAgent.{SingleWordInSentence, TaggedQuery}
import eiti.sag.query.KnownPosTags.KnownPosTags

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Await
import eiti.sag.query._
import opennlp.tools.postag.{POSModel, POSTaggerME}
import opennlp.tools.tokenize.WhitespaceTokenizer
import java.util.ArrayList

import eiti.sag.knowledge_agents.KnowledgeAgentsSupervisor.StartLearning
import opennlp.tools.lemmatizer.DictionaryLemmatizer

import scala.collection.JavaConverters._

class TranslationAgent extends Actor {
  val log = Logging(context.system, this)

  val lexiconFileName = "database/en-pos-maxent.bin"
  context.setReceiveTimeout(2 minutes)
  val model = new POSModel(new BufferedInputStream(new FileInputStream(lexiconFileName)))
  val tagger = new POSTaggerME(model)

  def mainMenu(): Unit = {
    println("What would you like me to do?")
    println("1 - Learn")
    println("2 - Explore")
    println("3 - Close")
    var task = scala.io.StdIn.readLine()
    task match {
      case "1" => self ! "askToLearn"
      case "2" => self ! "askAboutAnimal"
      case "3" =>
        context.system.actorSelection("/user/*") ! Kill
        sys.exit()
      case _ =>
        println("Please type 1, 2 or 3")
        mainMenu()
    }
  }

  // Get animal name from user
  def askExplore(): String = {
    println("Which animal are you interested in?")
    var animal = scala.io.StdIn.readLine()
    return animal
  }

  def getQuestion(animal:String): String = {
    println("What do you want to know about " + animal + "?")
    var infoType = scala.io.StdIn.readLine()
    println("Okay. Looking for " + infoType)
    return infoType
  }

  def getQuestionType(question: String): QueryType.Value = {

    for (word <- question.split(" +")){
      for ((k,v) <- QueryMap.keywordListToQueryTypeMap) if(k.contains(word)) return v
    }
    return QueryType.None
  }

  def tag(text: String) = {
    // znaczenie tag'ów: http://paula.petcu.tm.ro/init/default/post/opennlp-part-of-speech-tags
    // nie dzielimy na zdania, bo zakładamy, że zapytanie użytkownika jest już pojedynczynym zdaniem
    val whitespaceTokenizerLine: Array[String] = WhitespaceTokenizer.INSTANCE.tokenize(text)
    val tags: Array[String] = tagger.tag(whitespaceTokenizerLine)
    val tokenizedSentence = (tags zip whitespaceTokenizerLine).zipWithIndex map {
      case ((tag: String, word: String), index: Int) => SingleWordInSentence(word, tag, KnownPosTags.fromString(tag), index)
    }

    TaggedQuery(tokenizedSentence)
  }

  def getMainWord(tag:TaggedQuery,question:String):List[(String,String)] = {
    val whitespaceTokenizerLine: Array[String] = WhitespaceTokenizer.INSTANCE.tokenize(question)
    var foundWord = new ArrayList[(String,String)]
    whitespaceTokenizerLine(0) match {
      case "what" => {
        if (whitespaceTokenizerLine.length > 1) {
          whitespaceTokenizerLine(1) match {
            case "is" | "are" => {
              println("looking for noun")
              for (word <- tag.sentence){
                word.posRaw match {
                  case "NN" | "NNS" | "NNP" | "NNSP" => foundWord.add((word.word.replaceAll("[ \\?\\!,.]",""),word.posRaw))
                  case _ =>
                }
              }}
            case "do" | "does" | "did" => {
              println("looking for verb")
              for (word <- tag.sentence){
                word.posRaw match {
                  case "VB" | "VBD" | "VBG" | "VBN" | "VBP" | "VBZ" => foundWord.add((word.word.replaceAll("[ \\?\\!,.]",""), word.posRaw))
                  case _ =>
                }
              }
            }
          }
        }
        else {println("I cannot understand question")}
      }
      case "where" => {
        println("looking for verb")
        for (word <- tag.sentence){
          word.posRaw match {
            case "VB" | "VBD" | "VBG" | "VBN" | "VBP" | "VBZ" => foundWord.add((word.word.replaceAll("[ \\?\\!,.]",""), word.posRaw))
            case _ =>
          }
        }
      }
      case _ => println("I don't know what to do :(")
    }
    return foundWord.asScala.toList
  }

  def getMainLemmas(tag:TaggedQuery,question:String):Array[String] = {
    val mainWords = getMainWord(tag,question)
    val bis = new BufferedInputStream(new FileInputStream("database/en-lemmatizer.dict"))
    val lemmaModel = new DictionaryLemmatizer(bis)
    var tokens = new ArrayList[String]
    var postags = new ArrayList[String]
    for (word <- mainWords) {tokens.add(word._1); postags.add(word._2)}
    val lemma = lemmaModel.lemmatize(tokens.asScala.toArray, postags.asScala.toArray)
    for ((lemmaWord,i) <- lemma.zipWithIndex){
      if (lemmaWord == "O") lemma(i) = tokens.asScala.toList(i)
    }
    return lemma
  }

  // Choose one Agent with name matching pattern
  def choseOneAgent(patternName:String): ActorRef = {
    var test = context.actorSelection("akka://AnimalsKnowledgeBase/user/" + patternName + "*").resolveOne(5 second)
    var agent = Await.result(test,5 second)
    return agent
  }

  // Choose all Agents with name matching pattern
  def choseAllAgents(patternName:String): ActorSelection = {
    var agent = context.actorSelection("akka://AnimalsKnowledgeBase/user/" + patternName + "*")
    return agent
  }

  def askToLearn(): Unit = {
    var animals = askExplore().toLowerCase
    val animalsList = animals.replaceAll(" ","").split(",").toList
    choseOneAgent("KnowledgeAgentsSupervisor") ! StartLearning(animalsList)
    choseOneAgent("MetaKnowledge") ! "fetch"
  }

  def askAboutAnimals() = {
    val animal = askExplore().toLowerCase
    val question = getQuestion(animal)
    val questionType = getQuestionType(question)
    val tagged = tag(question)
    val mainWords = getMainLemmas(tagged,question)
    for ( i<-mainWords) println(i)

    if(questionType == null){
      log.info("Cannot resolve question type")
    }

    context.actorSelection("akka://AnimalsKnowledgeBase/user/KnowledgeAgentsSupervisor") ! UsersQueryInstance(animal, questionType, tagged, mainWords, animal)

  }

  // Receive Message cases
  def receive = {
    case "mainMenu" => mainMenu()
    case "askAboutAnimal" => askAboutAnimals()
    case "askToLearn" => askToLearn()
    case ReceiveTimeout =>
      println("I was waiting sooo long. Let's try again.")
      mainMenu()
    case _ => log.info("received unknown message")
  }
}

object TranslationAgent {
  case class SingleWordInSentence(word: String, posRaw: String, pos: KnownPosTags, index: Int)

  case class TaggedQuery(sentence: Array[SingleWordInSentence]) {
    assert(sentence.map(word => word.index).sorted sameElements  sentence.map(w => w.index))

  }
}