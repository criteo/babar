package com.bhnte.babar.processor

import com.bhnte.babar.api.metrics.Gauge
import com.trueaccord.scalapb.json.JsonFormat
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

  protected val LINE_PREFIX: String = "BABAR_METRIC\t"
  protected val LINE_PREFIX_LENGTH: Int = LINE_PREFIX.length

  def main(args: Array[String]): Unit = {

    // parse arguments
    val conf = new Conf(args)

    // define aggregations
    val timePrecMs = conf.timePrecision()
    val timePrecSec = timePrecMs / 1000D
    val MBSec = timePrecSec / 1024D / 1024D
    val allMemoryCpuAggregations = Set(
      SumMaxByContainerAggregation("HEAP_MEMORY_USED_BYTES", timePrecMs),
      MaxAggregation("HEAP_MEMORY_USED_BYTES", timePrecMs),
      SumMaxByContainerAggregation("OFF_HEAP_MEMORY_USED_BYTES", timePrecMs),
      MaxAggregation("OFF_HEAP_MEMORY_USED_BYTES", timePrecMs),
      SumMaxByContainerAggregation("HEAP_MEMORY_COMMITTED_BYTES", timePrecMs),
      MaxAggregation("HEAP_MEMORY_COMMITTED_BYTES", timePrecMs),
      SumMaxByContainerAggregation("OFF_HEAP_MEMORY_COMMITTED_BYTES", timePrecMs),
      MaxAggregation("OFF_HEAP_MEMORY_COMMITTED_BYTES", timePrecMs),
      SumMaxByContainerAggregation("MEMORY_RESERVED_BYTES", timePrecMs),
      MaxAggregation("MEMORY_RESERVED_BYTES", timePrecMs),
      MaxAggregation("JVM_SCALED_CPU_USAGE", timePrecMs),
      MedianMaxByContainerAggregation("JVM_SCALED_CPU_USAGE", timePrecMs),
      MaxAggregation("GC_RATIO", timePrecMs),
      MedianMaxByContainerAggregation("GC_RATIO", timePrecMs),
      CountContainersAggregation("NUM_CONTAINERS", timePrecMs),
      AccumulatedMaxByContainerAggregation("MEMORY_RESERVED_BYTES", timePrecMs, MBSec),
      AccumulatedMaxByContainerAggregation("HEAP_MEMORY_USED_BYTES", timePrecMs, MBSec),
      AccumulatedMaxByContainerAggregation("OFF_HEAP_MEMORY_USED_BYTES", timePrecMs, MBSec),
      AccumulatedMaxByContainerAggregation("JVM_SCALED_CPU_USAGE", timePrecMs, timePrecSec),
      AccumulatedMaxByContainerAggregation("GC_TIME_MS", timePrecMs, 1D / 1000D)

    )

    val allTracesAggregations = Set(
      TracesAggregation("CPU_TRACES", conf.tracesPrefixes().split(',').toSet, conf.tracesMinRatio())
    )

    val allAggregations = allMemoryCpuAggregations ++ allTracesAggregations

    println("start aggregating...")
    val stream = FSUtils
      .readAsStream(new Path(conf.logFile()))
      .flatMap(tryParseLine)
      .flatMap(_.toOption)

    val containers = conf.containers().split(',').toSet
    stream
      .filter{g =>
        if (containers.isEmpty) true
        else containers.exists(c => g.container.startsWith(c))
      }
      .foreach(g => allAggregations.foreach(agg => agg.process(g)))
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
    if (line != null && line.startsWith(LINE_PREFIX))
      Some(Try(JsonFormat.fromJsonString[Gauge](line.drop(LINE_PREFIX_LENGTH))))
    else
      None
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

case class SumMaxByContainerAggregation(metric: String, timePrecision: Long, multiplier: Double = 1D)
  extends TimeValueByContainerAggregation[Double, Double, Double] {
  override def name: String = metric+"(sum)"
  override def applies(g: Gauge): Boolean = g.metric == metric
  override def valueToV(v: Double): Double = v
  override def zeroByContainer: Double = 0D
  override def foldByContainer(acc: Double, v: Double): Double = math.max(acc, v)
  override def zeroOverAllContainers: Double = 0D
  override def foldOverAllContainers(acc: Double, v: Double): Double = acc + v
  override def aggByContainerToValue(agg: Double): Double = agg
  override def aggOverAllContainersToValue(agg: Double): Double = agg
}

case class AvgMaxByContainerAggregation(metric: String, timePrecision: Long, multiplier: Double = 1D)
  extends TimeValueByContainerAggregation[Double, (Long, Double), Double] {
  override def name: String = metric+"(avg)"
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

case class MedianMaxByContainerAggregation(metric: String, timePrecision: Long, multiplier: Double = 1D)
  extends TimeValueByContainerAggregation[Double, mutable.Buffer[Double], Double] {
  override def name: String = metric+"(med)"
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

case class AccumulatedMaxByContainerAggregation(metric: String, timePrecision: Long, multiplier: Double = 1D)
  extends TimeValueByContainerAggregation[Double, Double, Double] {
  override def name: String = metric+"(accumulated)"
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

case class MaxAggregation(metric: String, timePrecision: Long, multiplier: Double = 1D)
  extends TimeValueByContainerAggregation[Double, Double, Double] {
  override def name: String = metric+"(max)"
  override def applies(g: Gauge): Boolean = g.metric == metric
  override def valueToV(v: Double): Double = v
  override def zeroByContainer: Double = 0D
  override def zeroOverAllContainers: Double = 0D
  override def foldByContainer(acc: Double, v: Double): Double = math.max(acc, v)
  override def foldOverAllContainers(acc: Double, v: Double): Double = math.max(acc, v)
  override def aggByContainerToValue(agg: Double): Double = agg
  override def aggOverAllContainersToValue(agg: Double): Double = agg
}

case class CountContainersAggregation(name: String, timePrecision: Long, multiplier: Double = 1D)
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

case class TracesAggregation(metric: String,
                             tracesPrefixes: Set[String],
                             minSampleRatio: Double)
  extends Aggregation[TraceNode] {

  override def name: String = metric+"(traces)"

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
