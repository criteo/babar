package com.criteo.babar.processor

import org.rogach.scallop.ScallopConf

import scala.util.parsing.json.JSONObject

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val logFile = opt[String](short = 'l', required = true, descr = "the log file to open")
  val timePrecision = opt[Long](short = 't', descr = "time precision (in ms) to use in aggregations (default: 10000)", default = Some(10000L))
  val outputDir = opt[String](short = 'o', descr = "path of the output dir (default: ./output)", default = Some("./output"))
  val tracesMinRatio = opt[Double](short = 'm', descr = "min ratio of trace samples to keep trace (default: 0.001)", default = Some(0.001D))
  val tracesPrefixes = opt[String](short = 'p', descr = "if set, traces will be aggregated only from methods matching the prefixes (comma-separated)", default = Some(""))
  val containers = opt[String](short = 'c', descr = "if set, only metrics of containers matching these prefixes are aggregated (comma-separated)", default = Some(""))
  val tracesMaxDepth = opt[Int](short = 'd', descr = "Only methods with a depth in the call stack inferior or equal to this value will be kept. This is useful to prevent " +
    "creating deeply nested JSON objects that would make the renderer crash. To disable it, set a negative value (e.g. -1)", default = Some(100))
  verify()
}

object Processor {

  val LINE_PREFIX: String = "BABAR\t"
  val LINE_SEPARATOR = "\t"
  val LINE_PREFIX_LENGTH: Int = LINE_PREFIX.length

  def main(args: Array[String]): Unit = {

    // parse arguments
    val conf = new Conf(args)
    val allowedTracePrefixes = conf.tracesPrefixes().split(',').toSet
    val timePrecMs = conf.timePrecision()
    val timePrecSec = timePrecMs / 1000D
    val MB = 1D / 1024D / 1024D
    val MBSec = timePrecSec * MB

    // Aggregations to perform on the input stream
    val aggregations = Map[String, Aggregation2[Gauge, _]](
      // ----------------------------- General ----------------------------------
      "containers" ->
        (DiscretizeTime(timePrecMs) aggregate OneByContainerAndTime() and SumOverAllContainersByTime()),
      // ------------------------------ Memory ----------------------------------
      "total used heap" ->
        (FilterMetric("HEAP_MEMORY_USED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and SumOverAllContainersByTime()),
      "max used heap" ->
        (FilterMetric("HEAP_MEMORY_USED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MaxOverAllContainersByTime()),
      "total used off-heap" ->
        (FilterMetric("OFF_HEAP_MEMORY_USED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and SumOverAllContainersByTime()),
      "max used off-heap" ->
        (FilterMetric("OFF_HEAP_MEMORY_USED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MaxOverAllContainersByTime()),
      "total committed heap" ->
        (FilterMetric("HEAP_MEMORY_COMMITTED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and SumOverAllContainersByTime()),
      "max committed heap" ->
        (FilterMetric("HEAP_MEMORY_COMMITTED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MaxOverAllContainersByTime()),
      "total committed off-heap" ->
        (FilterMetric("OFF_HEAP_MEMORY_COMMITTED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and SumOverAllContainersByTime()),
      "max committed off-heap" ->
        (FilterMetric("OFF_HEAP_MEMORY_COMMITTED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MaxOverAllContainersByTime()),
      "total reserved" ->
        (FilterMetric("MEMORY_RESERVED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and SumOverAllContainersByTime()),
      "max reserved" ->
        (FilterMetric("MEMORY_RESERVED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MaxOverAllContainersByTime()),
      "accumulated reserved" ->
        (FilterMetric("MEMORY_RESERVED_BYTES") and Scale(MBSec) and DiscretizeTime(timePrecMs)
          aggregate AvgByContainerAndTime() and IntegrateOverAllContainersByTime()),
      // TODO change all RSS memory to use right key
      "total RSS memory" ->
        (FilterMetric("OFF_HEAP_MEMORY_USED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and SumOverAllContainersByTime()),
      "max RSS memory" ->
        (FilterMetric("OFF_HEAP_MEMORY_USED_BYTES") and Scale(MB) and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MaxOverAllContainersByTime()),
      "accumulated RSS memory" ->
        (FilterMetric("OFF_HEAP_MEMORY_USED_BYTES") and Scale(MBSec) and DiscretizeTime(timePrecMs)
          aggregate AvgByContainerAndTime() and IntegrateOverAllContainersByTime()),
      // ------------------------------ CPU ----------------------------------
      "max host CPU load" ->
        (FilterMetric("SYSTEM_CPU_LOAD") and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MaxOverAllContainersByTime()),
      "median host CPU load" ->
        (FilterMetric("SYSTEM_CPU_LOAD") and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MedianOverAllContainersByTime()),
      "max JVM CPU load" ->
        (FilterMetric("JVM_CPU_USAGE") and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MaxOverAllContainersByTime()),
      "median JVM CPU load" ->
        (FilterMetric("JVM_CPU_USAGE") and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MedianOverAllContainersByTime()),
      "median JVM CPU load" ->
        (FilterMetric("JVM_CPU_USAGE") and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MedianOverAllContainersByTime()),
      // TODO accumulated CPU Time
      // ------------------------------ GC ----------------------------------
      "max GC ratio" ->
        (FilterMetric("GC_RATIO") and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MaxOverAllContainersByTime()),
      "median GC ratio" ->
        (FilterMetric("GC_RATIO") and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MedianOverAllContainersByTime()),
      "median minor GC ratio" ->
        (FilterMetric("MINOR_GC_RATIO") and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MedianOverAllContainersByTime()),
      "median major GC ratio" ->
        (FilterMetric("MAJOR_GC_RATIO") and DiscretizeTime(timePrecMs)
          aggregate MaxByContainerAndTime() and MedianOverAllContainersByTime()),
      // TODO accumulated GC Time
      // ------------------------------ Traces ----------------------------------
      "traces" ->
        (FilterMetric("CPU_TRACES")
          aggregate TracesAggregation2(allowedTracePrefixes, conf.tracesMinRatio(), conf.tracesMaxDepth()))
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
    val json = buildMetricsJSON(aggregations)
    // TODO replace8
    IOUtils.copyFromResources("index.html", "test.html", json.toString())
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

  def buildMetricsJSON(aggregations: Map[String, Aggregation2[_, _]]): JSONObject = {
    val jsons = aggregations.map{ case (key, agg) => (key, agg.json()) }
    JSONObject(jsons)
  }
}
