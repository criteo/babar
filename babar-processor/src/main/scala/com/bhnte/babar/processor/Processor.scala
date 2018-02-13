package com.bhnte.babar.processor

import org.apache.commons.lang3.mutable.MutableLong
import org.apache.hadoop.fs.Path
import org.rogach.scallop.ScallopConf

import scala.collection.mutable
import scala.util.Try
import scala.util.parsing.json.{JSONArray, JSONObject}

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val logFile = opt[String](short = 'l', required = true, descr = "the log file to open")
  val timePrecision = opt[Long](short = 't', descr = "time precision (in ms) to use in aggregations (default: 10000)", default = Some(10000L))
  val outputDir = opt[String](short = 'o', descr = "path of the output dir (default: ./output)", default = Some("./output"))
  val tracesMinRatio = opt[Double](short = 'm', descr = "min ratio of trace samples to keep trace (default: 0.001)", default = Some(0.001D))
  val tracesPrefixes = opt[String](short = 'p', descr = "if set, traces will be aggregated only from methods matching the prefixes (comma-separated)", default = Some(""))
  val containers = opt[String](short = 'c', descr = "if set, only metrics of containers matching these prefixes are aggregated (comma-separated)", default = Some(""))
  verify()
}

object Processor {

  val MEMORY_CPU_HTML_FILE = "memory-cpu.html"
  val TRACES_HTML_FILE = "traces.html"

  val LINE_PREFIX: String = "BABAR\t"
  val LINE_SEPARATOR = "\t"
  val LINE_PREFIX_LENGTH: Int = LINE_PREFIX.length

  def main(args: Array[String]): Unit = {

    // parse arguments
    val conf = new Conf(args)

    // define aggregations
    val timePrecMs = conf.timePrecision()
    val timePrecSec = timePrecMs / 1000D
    val MB = 1D / 1024D / 1024D
    val MBSec = timePrecSec * MB
    val allMemoryCpuAggregations = Set(
      SumMaxByContainerAggregation("total used heap", "HEAP_MEMORY_USED_BYTES", timePrecMs, MB),
      MaxAggregation("max used heap", "HEAP_MEMORY_USED_BYTES", timePrecMs, MB),
      SumMaxByContainerAggregation("total used off-heap", "OFF_HEAP_MEMORY_USED_BYTES", timePrecMs, MB),
      MaxAggregation("max used off-heap", "OFF_HEAP_MEMORY_USED_BYTES", timePrecMs, MB),
      SumMaxByContainerAggregation("total committed heap", "HEAP_MEMORY_COMMITTED_BYTES", timePrecMs, MB),
      MaxAggregation("max committed heap", "HEAP_MEMORY_COMMITTED_BYTES", timePrecMs, MB),
      SumMaxByContainerAggregation("total committed off-heap", "OFF_HEAP_MEMORY_COMMITTED_BYTES", timePrecMs, MB),
      MaxAggregation("max committed off-heap", "OFF_HEAP_MEMORY_COMMITTED_BYTES", timePrecMs, MB),
      SumMaxByContainerAggregation("total reserved", "MEMORY_RESERVED_BYTES", timePrecMs, MB),
      MaxAggregation("max reserved", "MEMORY_RESERVED_BYTES", timePrecMs, MB),
      MaxAggregation("max JVM CPU usage", "JVM_SCALED_CPU_USAGE", timePrecMs),
      MedianMaxByContainerAggregation("median JVM CPU usage", "JVM_SCALED_CPU_USAGE", timePrecMs),
      MaxAggregation("max system CPU load", "SYSTEM_CPU_LOAD", timePrecMs),
      MedianMaxByContainerAggregation("median system CPU load", "SYSTEM_CPU_LOAD", timePrecMs),
      MaxAggregation("max GC ratio", "GC_RATIO", timePrecMs),
      MedianMaxByContainerAggregation("median GC ratio", "GC_RATIO", timePrecMs),
      MaxAggregation("max minor GC ratio", "MINOR_GC_RATIO", timePrecMs),
      MaxAggregation("max major GC ratio", "MAJOR_GC_RATIO", timePrecMs),
      MedianMaxByContainerAggregation("median minor GC ratio", "MINOR_GC_RATIO", timePrecMs),
      MedianMaxByContainerAggregation("median major GC ratio", "MAJOR_GC_RATIO", timePrecMs),
      CountContainersAggregation("containers", timePrecMs),
      AccumulatedAvgByContainerAggregation("accumulated reserved", "MEMORY_RESERVED_BYTES", timePrecMs, MBSec),
      AccumulatedAvgByContainerAggregation("accumulated used heap", "HEAP_MEMORY_USED_BYTES", timePrecMs, MBSec),
      AccumulatedAvgByContainerAggregation("accumulated used off-heap", "OFF_HEAP_MEMORY_USED_BYTES", timePrecMs, MBSec),
      AccumulatedAvgByContainerAggregation("accumulated JVM CPU sec", "JVM_SCALED_CPU_USAGE", timePrecMs, timePrecSec),
      AccumulatedAvgByContainerAggregation("accumulated GC CPU sec", "GC_SCALED_CPU_USAGE", timePrecMs, timePrecSec)
    )

    val allTracesAggregations = Set(
      TracesAggregation("traces", "CPU_TRACES", conf.tracesPrefixes().split(',').toSet, conf.tracesMinRatio())
    )

    val allAggregations = allMemoryCpuAggregations ++ allTracesAggregations
    val containers = conf.containers().split(',').toSet

    println("start aggregating...")
    FSUtils
      .readAsStream(new Path(conf.logFile()))
      .flatMap(tryParseLine)
      .flatMap(_.toOption)
      .foreach(g => {
        if (containers.isEmpty || containers.exists(c => g.container.startsWith(c))) {
          allAggregations.foreach(agg => agg.process(g))
        }
      })
    println("done aggregating")

    println("start writing memory & cpu html...")
    val memoryCpuJson = buidMemoryCpuJson(allMemoryCpuAggregations)
    FSUtils.copyFromResourceWithData(MEMORY_CPU_HTML_FILE, new Path(conf.outputDir(), MEMORY_CPU_HTML_FILE), memoryCpuJson.toString())
    println("done writing memory & cpu html")

    println("start writing traces html...")
    val tracesJson = buildTracesJson(allTracesAggregations)
    FSUtils.copyFromResourceWithData(TRACES_HTML_FILE, new Path(conf.outputDir(), TRACES_HTML_FILE), tracesJson.toString())
    println("done writing traces html")

  }

  protected def tryParseLine(line: String): Option[Try[Gauge]] = {
    if (line != null && line.startsWith(LINE_PREFIX)) {
      val splits = line.drop(LINE_PREFIX_LENGTH).split(LINE_SEPARATOR)
      if (splits.length < 4 || splits.length > 5) None
      else Some(Try(Gauge(
        container = splits(0),
        metric = splits(1),
        timestamp = splits(2).toLong,
        value = splits(3).toDouble,
        label = if (splits.length == 5) splits(4) else ""
      )))
    }
    else {
      None
    }
  }

  def buidMemoryCpuJson(memoryCpuAggregations: Set[_ <: Aggregation[Vector[(Long, Double)]]]): JSONObject = {
    buidMetricsJson(memoryCpuAggregations)
  }

  def buidMetricsJson(aggregations: Set[_ <: Aggregation[Vector[(Long, Double)]]]): JSONObject = {
    val computedAggregations = aggregations.map(agg => (agg.name, agg.get))
    val metrics = computedAggregations.map{ case (name, res) =>
      (name, JSONArray(res.map(_._2).toList))
    }
    JSONObject(Map(
      "time" -> JSONArray(computedAggregations.toList.maxBy(_._2.length)._2.map(_._1).toList),
      "metrics" -> JSONObject(metrics.toMap)
    ))
  }

  def buildTracesJson(tracesAggregations: Set[_ <: Aggregation[TraceNode]]): JSONObject = {
    val tracesMap = tracesAggregations.map(agg => (agg.name, agg.get.toJSON)).toMap
    JSONObject(tracesMap)
  }
}

case class Gauge(container: String,
                 metric: String,
                 timestamp: Long,
                 value: Double,
                 label: String) {
}

trait Aggregation[T] {
  def process(g: Gauge): Unit
  def get: T
  def name: String
}

trait TimeValueByContainerAggregation[A_CONT, A_ALL, V] extends Aggregation[Vector[(Long, V)]] {

  // this contains the time points for all metrics as we need them all to have the same time coordinates
  protected val times = mutable.Set[Long]()
  // Map(container -> Map(time -> value))
  protected val map = mutable.Map.empty[String, mutable.Map[Long, A_CONT]]

  def multiplier: Double
  def applies(g: Gauge): Boolean
  def timePrecision: Long
  def valueToV(v: Double): V
  def zeroByContainer: A_CONT
  def foldByContainer(acc: A_CONT, v: V): A_CONT
  def aggByContainerToValue(agg: A_CONT): V
  def zeroOverAllContainers: A_ALL
  def foldOverAllContainers(acc: A_ALL, v: V): A_ALL
  def aggOverAllContainersToValue(agg: A_ALL): V

  override def process(g: Gauge): Unit = {
    val t = floorTime(g.timestamp)
    times.add(t)
    if (applies(g)) {
      // get the map for the current container
      val cmap = map.getOrElse(g.container, mutable.Map.empty[Long, A_CONT])
      val acc = cmap.getOrElse(t, zeroByContainer)
      val v = valueToV(g.value * multiplier)
      cmap.put(t, foldByContainer(acc, v))
      // update the container map
      map.put(g.container, cmap)
    }
  }

  override def get: Vector[(Long, V)] = {
    // first, aggregate over all containers
    val aggOverAllContainers = map.values
      .flatMap(_.toVector)
      .groupBy(_._1)
      .map{ case (t, aggForTimeByContainer) =>
        val aggForTimeOverAllContainers = aggForTimeByContainer
          .map(a => aggByContainerToValue(a._2))
          .foldLeft(zeroOverAllContainers)((acc, v) => foldOverAllContainers(acc, v))
        (t, aggOverAllContainersToValue(aggForTimeOverAllContainers))
      }
    val zero = aggOverAllContainersToValue(zeroOverAllContainers)
    times.map(t => (t, aggOverAllContainers.getOrElse(t, zero))).toVector.sortBy(_._1)
  }

  protected def floorTime(timestamp: Long): Long = {
    timestamp - (timestamp % timePrecision)
  }
}

case class SumMaxByContainerAggregation(name: String,
                                        metric: String,
                                        timePrecision: Long,
                                        multiplier: Double = 1D)
  extends TimeValueByContainerAggregation[Double, Double, Double] {

  override def applies(g: Gauge): Boolean = g.metric == metric
  override def valueToV(v: Double): Double = v
  override def zeroByContainer: Double = 0D
  override def foldByContainer(acc: Double, v: Double): Double = math.max(acc, v)
  override def zeroOverAllContainers: Double = 0D
  override def foldOverAllContainers(acc: Double, v: Double): Double = acc + v
  override def aggByContainerToValue(agg: Double): Double = agg
  override def aggOverAllContainersToValue(agg: Double): Double = agg
}

case class AvgMaxByContainerAggregation(name: String,
                                        metric: String,
                                        timePrecision: Long,
                                        multiplier: Double = 1D)
  extends TimeValueByContainerAggregation[Double, (Long, Double), Double] {

  override def applies(g: Gauge): Boolean = g.metric == metric
  override def valueToV(v: Double): Double = v
  override def zeroByContainer: Double = 0D
  override def foldByContainer(acc: Double, v: Double): Double = math.max(acc, v)
  override def zeroOverAllContainers: (Long, Double) = (0L, 0D)
  override def foldOverAllContainers(acc: (Long, Double), v: Double): (Long, Double) = (acc._1 + 1, acc._2 + v)
  override def aggByContainerToValue(agg: Double): Double = agg
  //average over the max of all containers
  override def aggOverAllContainersToValue(acc: (Long, Double)): Double = acc._2 / acc._1
}

case class MedianMaxByContainerAggregation(name: String,
                                           metric: String,
                                           timePrecision: Long,
                                           multiplier: Double = 1D)
  extends TimeValueByContainerAggregation[Double, mutable.Buffer[Double], Double] {

  override def applies(g: Gauge): Boolean = g.metric == metric
  override def valueToV(v: Double): Double = v
  override def zeroByContainer: Double = 0D
  override def foldByContainer(acc: Double, v: Double): Double = math.max(acc, v)
  override def zeroOverAllContainers: mutable.Buffer[Double] = mutable.ArrayBuffer[Double]()
  override def foldOverAllContainers(acc: mutable.Buffer[Double], v: Double): mutable.Buffer[Double] = {acc.append(v); acc}
  override def aggByContainerToValue(agg: Double): Double = agg
  //average over the max of all containers
  override def aggOverAllContainersToValue(acc: mutable.Buffer[Double]): Double = {
    val size = acc.size
    if (size == 0) zeroByContainer
    else {
      val sorted = acc.sorted
      sorted(size/2)
    }
  }
}

case class AccumulatedMaxByContainerAggregation(name: String,
                                                metric: String,
                                                timePrecision: Long,
                                                multiplier: Double = 1D)
  extends TimeValueByContainerAggregation[Double, Double, Double] {

  override def applies(g: Gauge): Boolean = g.metric == metric
  override def valueToV(v: Double): Double = v
  override def zeroByContainer: Double = 0D
  override def foldByContainer(acc: Double, v: Double): Double = math.max(acc, v)
  override def zeroOverAllContainers: Double = 0D
  override def foldOverAllContainers(acc: Double, v: Double): Double = acc + v
  override def aggByContainerToValue(agg: Double): Double = agg
  //average over the max of all containers
  override def aggOverAllContainersToValue(acc: Double): Double = acc

  override def get: Vector[(Long, Double)] = {
    val sortedBeforeAccumulated = super.get
    // accumulate the values in order to get the integral values
    sortedBeforeAccumulated.scanLeft((0L, zeroOverAllContainers)){
      case ((prevT, prev), (t, v)) => (t, prev + v)
    }.drop(1) // remove zero
  }
}

case class AccumulatedAvgByContainerAggregation(name: String,
                                                metric: String,
                                                timePrecision: Long,
                                                multiplier: Double = 1D)
  extends TimeValueByContainerAggregation[(Long, Double), Double, Double] {



  override def get: Vector[(Long, Double)] = {
    val sortedBeforeAccumulated = super.get
    // accumulate the values in order to get the integral values
    sortedBeforeAccumulated.scanLeft((0L, zeroOverAllContainers)){
      case ((prevT, prev), (t, v)) => (t, prev + v)
    }.drop(1) // remove zero
  }

  override def applies(g: Gauge): Boolean = g.metric == metric
  override def valueToV(v: Double): Double = v
  override def zeroByContainer: (Long, Double) = (0L, 0D)
  override def foldByContainer(acc: (Long, Double), v: Double): (Long, Double) = (acc._1 + 1L, acc._2 + v)
  override def aggByContainerToValue(agg: (Long, Double)): Double = agg._2 / agg._1
  override def zeroOverAllContainers: Double = 0D
  override def foldOverAllContainers(acc: Double, v: Double): Double = acc  + v
  override def aggOverAllContainersToValue(agg: Double): Double = agg
}

case class MaxAggregation(name: String,
                          metric: String,
                          timePrecision: Long,
                          multiplier: Double = 1D)
  extends TimeValueByContainerAggregation[Double, Double, Double] {

  override def applies(g: Gauge): Boolean = g.metric == metric
  override def valueToV(v: Double): Double = v
  override def zeroByContainer: Double = 0D
  override def zeroOverAllContainers: Double = 0D
  override def foldByContainer(acc: Double, v: Double): Double = math.max(acc, v)
  override def foldOverAllContainers(acc: Double, v: Double): Double = math.max(acc, v)
  override def aggByContainerToValue(agg: Double): Double = agg
  override def aggOverAllContainersToValue(agg: Double): Double = agg
}

case class CountContainersAggregation(name: String,
                                      timePrecision: Long,
                                      multiplier: Double = 1D)
  extends TimeValueByContainerAggregation[Double, Double, Double] {

  override def valueToV(v: Double): Double = v
  override def zeroByContainer: Double = 0L
  override def zeroOverAllContainers: Double = 0L
  override def foldByContainer(acc: Double, v: Double): Double = 1D
  override def foldOverAllContainers(acc: Double, v: Double): Double = acc + v
  override def applies(g: Gauge): Boolean = true
  override def aggByContainerToValue(agg: Double): Double = agg
  override def aggOverAllContainersToValue(agg: Double): Double = agg
}

case class TracesAggregation(name: String,
                             metric: String,
                             tracesPrefixes: Set[String],
                             minSampleRatio: Double)
  extends Aggregation[TraceNode] {

  private val root = TraceNode("root")

  override def process(g: Gauge): Unit = {
    if (g.metric != metric) return

    val splits = g.label.split('|')
      .drop(1) // drop the thread name as we aggregate over all threads
      // drop until we match a custom prefix
      .dropWhile( m => tracesPrefixes.nonEmpty && !tracesPrefixes.exists(m.startsWith))

    val samplesCount = g.value.toLong

    val leaf = splits.foldLeft(root){(parent, method) =>
      parent.inc(samplesCount)
      val maybeChildren = parent.children.get(method)
      if (maybeChildren.isDefined) {
        maybeChildren.get
      }
      else {
        val n = TraceNode(method)
        parent.children.put(method, n)
        n
      }
    }
    // increment count in the leaf
    if (leaf != root) leaf.inc(samplesCount)
  }

  override def get: TraceNode = {
    val minSamples = (root.value.getValue * minSampleRatio).toLong
    if (minSamples > 1) pruneTooLittleSamples(root, minSamples)
    root
  }

  private def pruneTooLittleSamples(n: TraceNode, minSamples: Long): Unit = {
    n.children.toList.foreach{ case (key, node) =>
        if (n.value.getValue < minSamples) n.children.remove(key)
    }
    n.children.foreach{ case (key, node) => pruneTooLittleSamples(node, minSamples)}
  }
}

case class TraceNode(name: String,
                     value: MutableLong = new MutableLong(0L),
                     children: mutable.Map[String, TraceNode] = mutable.Map.empty) {
  def inc(v: Long): Long = value.addAndGet(v)

  def toJSON: JSONObject = {
    JSONObject(Map(
      "name" -> name,
      "value" -> value.getValue,
      "children" -> JSONArray(children.values.toList.map(_.toJSON))
    ))
  }
}
