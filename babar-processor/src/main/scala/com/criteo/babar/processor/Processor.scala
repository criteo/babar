package com.criteo.babar.processor

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import org.rogach.scallop.ScallopConf

import scala.util.parsing.json.JSONObject

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

  val timePrecision = opt[Long](short = 't', descr = "time precision (in ms) to use in aggregations", default = Some(10000L))
  val outputFile = opt[String](short = 'o', descr = "path of the output file (default: ./babar_{date}.html)", default = Some(s"babar_${formatter.format(LocalDateTime.now())}.html"))
  val containers = opt[String](short = 'c', descr = "if set, only metrics of containers matching these prefixes are aggregated (comma-separated)", default = Some(""))
  val maxTracesDepth = opt[Int](short = 'd', descr = "max depth of stack traces", default = Some(100))
  val minTracesRatio = opt[Double](short = 'r', descr = "min ratio of appearance in profiles for traces to be kept", default = Some(0.001))
  val logFile = trailArg[String](descr = "the log file to open")
  verify()
}

object Processor {

  val LINE_PREFIX: String = "BABAR\t"
  val LINE_SEPARATOR = "\t"
  val LINE_PREFIX_LENGTH: Int = LINE_PREFIX.length

  val REPORT_TEMPLATE_FILE = "index.html"

  val MIN_TRACES_RATIO = 0.001 // keep only traces that appear in more tham 0.1% of profiles

  def main(args: Array[String]): Unit = {

    // parse arguments
    val conf = new Conf(args)
    val timePrecMs = conf.timePrecision()
    val sec = 1 / 1000D
    val timePrecSec = timePrecMs * sec
    val MB = 1D / 1024D / 1024D
    val MBSec = timePrecSec * MB

    // Aggregations to perform on the input stream
    val aggregations = Map[String, Aggregation2[Gauge, _]](
      // ----------------------------- General ----------------------------------
      "containers" ->
        (DiscretizeTime(timePrecMs) aggregate OneByContainerAndTime() and SumOverAllContainersByTime()),
      // ------------------------------ Memory ----------------------------------
      // Used
      "total used heap" ->
        (FilterMetric("JVM_HEAP_MEMORY_USED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and SumOverAllContainersByTime()),
      "max used heap" ->
        (FilterMetric("JVM_HEAP_MEMORY_USED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MaxOverAllContainersByTime()),
      "accumulated used heap" ->
        (FilterMetric("JVM_HEAP_MEMORY_USED_BYTES") and Scale(MBSec) and DiscretizeTime(timePrecMs)
          aggregate AvgByContainerAndTime() and AccumulateOverAllContainersByTime()),
      "total used off-heap" ->
        (FilterMetric("JVM_OFF_HEAP_MEMORY_USED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and SumOverAllContainersByTime()),
      "max used off-heap" ->
        (FilterMetric("JVM_OFF_HEAP_MEMORY_USED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MaxOverAllContainersByTime()),
      "accumulated used off-heap" ->
        (FilterMetric("JVM_OFF_HEAP_MEMORY_USED_BYTES") and Scale(MBSec) and DiscretizeTime(timePrecMs)
          aggregate AvgByContainerAndTime() and AccumulateOverAllContainersByTime()),
      // Committed
      "total committed heap" ->
        (FilterMetric("JVM_HEAP_MEMORY_COMMITTED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and SumOverAllContainersByTime()),
      "max committed heap" ->
        (FilterMetric("JVM_HEAP_MEMORY_COMMITTED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MaxOverAllContainersByTime()),
      "total committed off-heap" ->
        (FilterMetric("JVM_OFF_HEAP_MEMORY_COMMITTED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and SumOverAllContainersByTime()),
      "max committed off-heap" ->
        (FilterMetric("JVM_OFF_HEAP_MEMORY_COMMITTED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MaxOverAllContainersByTime()),
      // Reserved
      "total reserved" ->
        (FilterMetric("JVM_MEMORY_RESERVED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and SumOverAllContainersByTime()),
      "max reserved" ->
        (FilterMetric("JVM_MEMORY_RESERVED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MaxOverAllContainersByTime()),
      "accumulated reserved" ->
        (FilterMetric("JVM_MEMORY_RESERVED_BYTES") and Scale(MBSec) and DiscretizeTime(timePrecMs)
          aggregate AvgByContainerAndTime() and AccumulateOverAllContainersByTime()),
      // RSS
      "total RSS memory" ->
        (FilterMetric("PROC_RSS_MEMORY_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and SumOverAllContainersByTime()),
      "max RSS memory" ->
        (FilterMetric("PROC_RSS_MEMORY_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MaxOverAllContainersByTime()),
      "accumulated RSS memory" ->
        (FilterMetric("PROC_RSS_MEMORY_BYTES") and Scale(MBSec) and DiscretizeTime(timePrecMs)
          aggregate AvgByContainerAndTime() and AccumulateOverAllContainersByTime()),
      // ------------------------------ CPU ----------------------------------
      "max host CPU load" ->
        (FilterMetric("JVM_HOST_CPU_USAGE") and DiscretizeTime(timePrecMs)
          aggregate AvgByContainerAndTime() and MaxOverAllContainersByTime()),
      "median host CPU load" ->
        (FilterMetric("JVM_HOST_CPU_USAGE") and DiscretizeTime(timePrecMs)
          aggregate AvgByContainerAndTime() and MedianOverAllContainersByTime()),
      "max JVM CPU load" ->
        (FilterMetric("JVM_CPU_USAGE") and DiscretizeTime(timePrecMs)
          aggregate AvgByContainerAndTime() and MaxOverAllContainersByTime()),
      "median JVM CPU load" ->
        (FilterMetric("JVM_CPU_USAGE") and DiscretizeTime(timePrecMs)
          aggregate AvgByContainerAndTime() and MedianOverAllContainersByTime()),
      "median JVM CPU load" ->
        (FilterMetric("JVM_CPU_USAGE") and DiscretizeTime(timePrecMs)
          aggregate AvgByContainerAndTime() and MedianOverAllContainersByTime()),
      "accumulated JVM CPU time" ->
        (FilterMetric("JVM_CPU_TIME") and DiscretizeTime(timePrecMs) and Scale(sec)
          aggregate SumByContainerAndTime() and AccumulateOverAllContainersByTime()),
      // ------------------------------ GC ----------------------------------*/
      "max GC ratio" ->
        (FilterMetric("JVM_GC_RATIO") and Cap(0D, 1D) and DiscretizeTime(timePrecMs)
          aggregate AvgByContainerAndTime() and MaxOverAllContainersByTime()),
      "median GC ratio" ->
        (FilterMetric("JVM_GC_RATIO") and Cap(0D, 1D) and DiscretizeTime(timePrecMs)
          aggregate AvgByContainerAndTime() and MedianOverAllContainersByTime()),
      "median minor GC ratio" ->
        (FilterMetric("JVM_MINOR_GC_RATIO") and Cap(0D, 1D) and DiscretizeTime(timePrecMs)
          aggregate AvgByContainerAndTime() and MedianOverAllContainersByTime()),
      "median major GC ratio" ->
        (FilterMetric("JVM_MAJOR_GC_RATIO") and Cap(0D, 1D) and DiscretizeTime(timePrecMs)
          aggregate AvgByContainerAndTime() and MedianOverAllContainersByTime()),
      "accumulated GC CPU time" ->
        (FilterMetric("JVM_GC_CPU_TIME") and DiscretizeTime(timePrecMs) and Scale(sec)
          aggregate SumByContainerAndTime() and AccumulateOverAllContainersByTime()),
      // ------------------------------ Traces ----------------------------------
      "traces" ->
        (FilterMetric("CPU_TRACES")
          aggregate TracesAggregation2(conf.minTracesRatio(), conf.maxTracesDepth()))
    )

    val containers = conf.containers().split(',').toSet

    println("start aggregating...")
    HDFSUtils
      .readAsStreamWithProgressBar(conf.logFile())
      .flatMap(parseLine)
      .filter(filterContainer(containers))
      .foreach{ gauge =>
        aggregations.values.foreach(_.aggregate(gauge))
      }
    println("\ndone aggregating")

    println("Building json")
    val json = buildJSON(aggregations)
    IOUtils.copyFromResources(REPORT_TEMPLATE_FILE, conf.outputFile(), json.toString())
    println("done writing json")
  }

  protected def parseLine(line: String): Option[Gauge] = {
    if (line != null && line.startsWith(LINE_PREFIX)) {
      Gauge.tryParse(line.drop(LINE_PREFIX_LENGTH)).toOption
    }
    else {
      None
    }
  }

  def filterContainer(containers: Set[String])(gauge: Gauge): Boolean = {
    containers.isEmpty || containers.exists(c => gauge.container.startsWith(c))
  }

  def buildJSON(aggregations: Map[String, Aggregation2[_, _]]): JSONObject = {
    val jsons = aggregations.flatMap{ case (key, agg) => agg.json().map((key, _))}
    JSONObject(jsons)
  }
}
