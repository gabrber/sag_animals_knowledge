package eiti.sag.knowledge_agents

import java.io._
import java.nio.file.{Files, Paths}
import java.util

import akka.actor.Actor
import akka.event.Logging
import akka.util.Timeout
import eiti.sag.knowledge_agents.KnowledgeAgent.FetchedAlreadyLearnedAnimals
import eiti.sag.query.{QueryType, UsersQueryInstance}
import opennlp.tools.lemmatizer.LemmatizerME
import opennlp.tools.namefind.{NameFinderME, TokenNameFinderModel}
import opennlp.tools.ngram.NGramModel
import opennlp.tools.postag.{POSModel, POSSample, POSTaggerME}
import opennlp.tools.tokenize.{TokenizerME, TokenizerModel, WhitespaceTokenizer}
import opennlp.tools.util.StringList
import opennlp.tools.lemmatizer.DictionaryLemmatizer
import opennlp.tools.sentdetect.{SentenceDetectorME, SentenceModel}
import opennlp.tools.chunker.{ChunkerME, ChunkerModel}
import org.jsoup.nodes.{Node, TextNode}
import org.jsoup.select.NodeVisitor

import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.io.Source
import scala.language.postfixOps
import scalaj.http._
//https://github.com/ruippeixotog/scala-scraper
import net.ruippeixotog.scalascraper.browser.JsoupBrowser

//JSOUP
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

abstract class KnowledgeAgent extends Actor {
  val log = Logging(context.system, this)
  implicit val timeout = Timeout(10 minutes)

  val knowledgeBaseSep = ";"

  val locationModelFile = "database/en-ner-location.bin"
  val tokenModelFile = "database/en-token.bin"
  val posModelFile = "database/en-pos-maxent.bin"
  val lemmaModelFile = "database/en-lemmatizer.dict"
  val sentModelFile = "database/en-sent.bin"
  val chunkerModelFile = "database/en-chunker.bin"
  val HEURISTIC_CONTENT_LENGTH_THRESHOLD = 100

  var animalsLearnedAbout: List[String] = List()

  context.setReceiveTimeout(2 minutes)

  def learnAbout(animalUrl :String, animal :String, bag_of_words: String, ner :String, pos_ngrams: String, sentences: String, lemmaSentences : String, chunker : String)={

    val pageContent = fetchContent(animalUrl)
    persistAsBagOfWords(pageContent, animal, bag_of_words)
    persistAsNERTokens(pageContent, animal, ner)
    persistAsPosNgrams(pageContent, animal, pos_ngrams)
    persistAsSentences(pageContent, animal, sentences)
    persistAsLemmaSentences(sentences, animal, lemmaSentences)
    persistAsChunker(pageContent, animal, chunker)
  }

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

    if(usersQueryInstance.parsedType.equals(QueryType.Location)) {
      findLocationUsingNERTags(usersQueryInstance, dirname)
    } else {
      println("Sorry, cant answer")
    }
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

  def createNgram(pageContent: String) = {
    val tokens = WhitespaceTokenizer.INSTANCE.tokenize(pageContent)
    //val tokens = tokenize(pageContent)
    val nGramModel = new NGramModel
    nGramModel.add(new StringList(tokens: _*),2,3)
    //println("Total ngrams: " + nGramModel.numberOfGrams)
    nGramModel.toDictionary()
  }

  def persistAsPOS(pageContent: String) = {
    val pos = new BufferedInputStream(new FileInputStream(posModelFile))
    val posModel = new POSModel(pos)
    val posTagger = new POSTaggerME(posModel)
    val tokens = WhitespaceTokenizer.INSTANCE.tokenize(pageContent)
    val tags = posTagger.tag(tokens)
    val posSentence = new POSSample(tokens, tags)
    posSentence
  }

  def persistAsPosNgrams(pageContent :String,animal :String,dirname :String) = {

    val posSentenceString = persistAsPOS(pageContent).toString()
    val file = new File("animal_db/" + dirname + "/" + animal + ".txt")
    val bw = new BufferedWriter(new FileWriter(file))

    val ngramPos = createNgram(posSentenceString).asScala.toArray
    for(ngramList <- ngramPos) {
      for (i <- ngramList.asScala.toArray)
        for (elem <- i.split(","))
          //for (i <- elem.split("_")) bw.write(i + knowledgeBaseSep)
          bw.write(elem + knowledgeBaseSep)
      bw.write("\n")
    }
    bw.close()


//    val file = new File("animal_db/" + dirname + "/" + animal + ".txt")
//    val bw = new BufferedWriter(new FileWriter(file))
//    for (elem <- locationToWeightedCertaintyMap.keys) {
//      bw.write(elem + knowledgeBaseSep + locationToWeightedCertaintyMap(elem) + "\n")
//    }
//    bw.close()
  }

  def persistAsSentences(pageContent :String,animal :String,dirname :String) = {

    val sent = new BufferedInputStream(new FileInputStream(sentModelFile))
    val sentModel = new SentenceModel(sent)
    val sentences = new SentenceDetectorME(sentModel)
    val readSentece = sentences.sentDetect(pageContent)

    val file = new File("animal_db/" + dirname + "/" + animal + ".txt")
    val bw = new BufferedWriter(new FileWriter(file))

    for(sentence <- readSentece) {
          bw.write(sentence + "\n")
    }
    bw.close()
  }

  def persistAsLemmaSentences(filename:String,animal :String,dirname :String) = {

    val readFile = Source.fromFile("animal_db/" + filename + "/" + animal + ".txt")
    val file = new File("animal_db/" + dirname + "/" + animal + ".txt")
    val bw = new BufferedWriter(new FileWriter(file))

    //fixme - nowy POS i Lemmatizer inicjalizują się dla każdej linijki - czasochłonne
    for (line <- readFile.getLines) {
      for (lemma <- getLemma(line) :Array[String]) bw.write(lemma + knowledgeBaseSep)
      bw.write("\n")
    }
    readFile.close
    bw.close()
  }

  def getPos(pageContent: String) = {
    val pos = new BufferedInputStream(new FileInputStream(posModelFile))
    val posModel = new POSModel(pos)
    val posTagger = new POSTaggerME(posModel)
    //val tokens = WhitespaceTokenizer.INSTANCE.tokenize(pageContent)
    val tokens = tokenize(pageContent)
    val tags = posTagger.tag(tokens)
    (tokens,tags)
  }

  def getLemma(pageContent:String):Array[String] = {
    val bis = new BufferedInputStream(new FileInputStream(lemmaModelFile))
    val lemmaModel = new DictionaryLemmatizer(bis)
    val readPOS = getPos(pageContent)
    val tokens = readPOS._1
    val postags = readPOS._2
    val lemma = lemmaModel.lemmatize(tokens, postags)
    for ((lemmaWord,i) <- lemma.zipWithIndex){
      if (lemmaWord == "O") lemma(i) = tokens.toList(i)
    }
    lemma
  }

  def persistAsChunker(pageContent:String,animal :String,dirname :String) = {
    val bis  = new BufferedInputStream(new FileInputStream(chunkerModelFile))
    val chunkerModel = new ChunkerModel(bis)
    val chunker = new ChunkerME(chunkerModel)
    val readPOS = getPos(pageContent)
    val posSentence = readPOS._1
    val posTags = readPOS._2
    val chunkerText = chunker.chunk(posSentence,posTags)

    val file = new File("animal_db/" + dirname + "/" + animal + ".txt")
    val bw = new BufferedWriter(new FileWriter(file))
    
    for((chunk,i) <- chunkerText.zipWithIndex) {
      bw.write(posSentence(i)+ knowledgeBaseSep + chunk + "\n")
    }
    bw.close()

  }

  def findSentence(mainLemma :Array[String], animal : String, lemma_dir :String, sent_dir :String) :Array[String] = {
    var sentences = new util.ArrayList[String]

    val readLemma = Source.fromFile("animal_db/" + lemma_dir + "/" + animal + ".txt")
    //val readSent = Source.fromFile("animal_db/" + lemma_dir + "/" + sent_dir + ".txt")
    val readSent = Files.readAllLines(Paths.get("animal_db/" + sent_dir + "/" + animal + ".txt"))

    for ((line,i) <- readLemma.getLines.zipWithIndex) {
      var checkLine = line.split(knowledgeBaseSep)
      for (lemma <- mainLemma) if (checkLine.contains(lemma)) sentences.add(readSent.get(i))
    }
    readLemma.close
    for(i <- sentences.asScala.toArray) println(i)
    return sentences.asScala.toArray
  }

  def fetchAlreadLearnedAnimals(fileName: String)  = {
    val lines = Source.fromFile("animal_db/" + fileName).mkString.split("\n").filter(p => p.isEmpty == false)

    animalsLearnedAbout = lines.map(line => line.trim).toList
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

  def persistAnimalsLearnedAbout(animalsLearnedAbout: List[String], fileName: String) = {
    val file = new File("animal_db/" + fileName)
    val bw = new BufferedWriter(new FileWriter(file))

    for (elem <- animalsLearnedAbout) {
      bw.write(elem + "\n")
    }
    bw.close()
  }

  override def postStop(): Unit = {
    println("Agent postStop method")
    super.postStop()
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    println("Agent preRestart method")
    super.preRestart(reason, message)
  }

  override def postRestart(reason: Throwable): Unit = {
    println("Agent restarted")
    self ! FetchedAlreadyLearnedAnimals()
  }

  def kaboom() = {
    1 / 0
  }
}

object KnowledgeAgent {
  final case class LearnAbout(animal: String)
  final case class FetchedAlreadyLearnedAnimals()
}
