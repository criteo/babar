package com.criteo.babar.processor

import scala.util.Try

case class Gauge(container: String,
                 metric: String,
                 timestamp: Long,
                 value: Double,
                 label: String) {
}

object Gauge {

  val LINE_SEPARATOR = "\t"

  def tryParse(line: String): Try[Gauge] = {
    val splits = line.split(LINE_SEPARATOR)
    Try {
      Gauge(
        container = splits(0),
        metric = splits(1),
        timestamp = splits(2).toLong,
        value = splits(3).toDouble,
        label = if (splits.length == 5) splits(4) else ""
      )
    }
  }
}
