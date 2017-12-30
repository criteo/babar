package com.bhnte.babar.scalding.wordcount

import com.twitter.scalding._

/**
  * Example job computing word frequencies in a text file.
  * See bin/scalding-wordfreq.sh for how to run this job.
  */
class WordFreqJob(args: Args) extends BabarJob(args) {

  val linesPipe = TypedPipe
    .from(TextLine(args("input")))
    .flatMap(tokenize)

  val wordCounts = linesPipe
    .groupBy { word => word } // use each word for a key
    .size // in each group, get the size

  val totalWords = linesPipe.groupAll.size

   val wordAverage = wordCounts
     .cross(totalWords)
     .map{ case (wc, tc) => (wc._1, wc._2.toDouble/tc._2) }
     .groupAll
     .reduce { (l, r) => ("", l._2 + r._2)}
     .values
     .write(TypedTsv[(String, Double)](args("output")))

  // Split a piece of text into individual words.
  def tokenize(text: String): Array[String] = {
    // Lowercase each word and remove punctuation.
    text.toLowerCase.replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+")
  }
}
