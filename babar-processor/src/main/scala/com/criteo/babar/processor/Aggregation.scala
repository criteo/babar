package com.criteo.babar.processor

import org.apache.commons.lang3.mutable.MutableLong

import scala.collection.mutable
import scala.util.parsing.json.{JSONArray, JSONObject}


trait Transformation2[-IN, +OUT] {

  def transform(value: IN): Iterable[OUT]

  def and[NOUT](next: Transformation2[OUT, NOUT]): Transformation2[IN, NOUT] = {
    (value: IN) => this.transform(value).flatMap(next.transform)
  }

  def aggregate[NOUT](aggregation: Aggregation2[OUT, NOUT]): Aggregation2[IN, NOUT] = {
    val prev = this
    new Aggregation2[IN, NOUT] {
      override def aggregate(value: IN): Unit = transform(value).foreach(aggregation.aggregate)
      override def values(): Iterable[NOUT] = aggregation.values()
      override def json(): JSONObject = aggregation.json()
    }
  }
}

case class FilterMetric(metric: String) extends Transformation2[Gauge, Gauge] {
  override def transform(g: Gauge): Iterable[Gauge] = if (metric == g.metric) Some(g) else None
}

case class Scale(scale: Double) extends Transformation2[Gauge, Gauge] {
  override def transform(g: Gauge): Iterable[Gauge] = {
    Some(g.copy(value = g.value * scale))
  }
}

case class DiscretizeTime(precision: Long) extends Transformation2[Gauge, Gauge] {
  override def transform(g: Gauge): Iterable[Gauge] = {
    Some(g.copy(timestamp = g.timestamp - (g.timestamp % precision)))
  }
}


trait Aggregation2[-IN, +OUT] {

  def aggregate(value: IN): Unit

  def values(): Iterable[OUT]

  def json(): JSONObject

  def and[NOUT](next: Aggregation2[OUT, NOUT]): Aggregation2[IN, NOUT] = {
    val prev = this

    new Aggregation2[IN, NOUT] {
      override def aggregate(value: IN): Unit = {
        prev.aggregate(value)
      }
      override def values(): Iterable[NOUT] = {
        prev.values().foreach(next.aggregate)
        next.values()
      }
      override def json(): JSONObject = {
        values()
        next.json()
      }
    }
  }
}

class AggregationByContainerAndTime[ACC, OUT](val zero: ACC)(val fn: (ACC, Double) => ACC)(val fin: ACC => OUT) extends Aggregation2[Gauge, ((String, Long), OUT)] {

  private val map = mutable.Map[(String, Long), ACC]()

  override def aggregate(g: Gauge): Unit = {
    val prev = map.getOrElseUpdate((g.container, g.timestamp), zero)
    map.put((g.container, g.timestamp), fn(prev, g.value))
  }

  override def values(): Iterable[((String, Long), OUT)] = map.mapValues(fin)

  override def json(): JSONObject = {
    val res = values()
    val byContainer = res
      .map{ case ((container, time), value) => (container, time, value) }
      .groupBy(_._1)
      .map{ case (container, timeValues) =>
          val times = timeValues.map(_._2).toList
          val vals = timeValues.map(_._3).toList
        container -> Map("time" -> JSONArray(times), "values" -> JSONArray(vals))
      }
    JSONObject(byContainer)
  }
}

case class OneByContainerAndTime() extends AggregationByContainerAndTime(0D)((_, _) => 1D)(identity)

case class MaxByContainerAndTime() extends AggregationByContainerAndTime(0D)(Math.max)(identity)

case class MinByContainerAndTime() extends AggregationByContainerAndTime(0D)(Math.min)(identity)

case class AvgByContainerAndTime() extends AggregationByContainerAndTime((0D, 0L))(
  { case ((sum, count), v) => (sum + v, count + 1L)})(
  { case (sum, count) => sum / count })

class AggregationOverAllContainersByTime[V, ACC, OUT](zero: ACC)(val acc: (ACC, V) => ACC)(val fin: ACC => OUT)  extends Aggregation2[((String, Long), V), (Long, OUT)] {

  private val map = mutable.SortedMap[Long, ACC]()

  override def aggregate(tuple: ((String, Long), V)): Unit = {
    val ((_, time), value) = tuple
    val prev = map.getOrElseUpdate(time, zero)
    map.put(time, acc(prev, value))
  }

  override def values(): Iterable[(Long, OUT)] = map.mapValues(fin)

  override def json(): JSONObject = {
    val res = values()
    val times = res.map(_._1).toList
    val vals = res.map(_._2).toList
    JSONObject(Map("time" -> JSONArray(times), "values" -> JSONArray(vals)))
  }
}

case class SumOverAllContainersByTime()
  extends AggregationOverAllContainersByTime[Double, Double, Double](0D)((acc: Double, v: Double) => acc + v)(identity)

case class MaxOverAllContainersByTime()
  extends AggregationOverAllContainersByTime[Double, Double, Double](0D)(Math.max)(identity)

case class MedianOverAllContainersByTime()
  extends AggregationOverAllContainersByTime[Double, Vector[Double], Double](Vector.empty)(
    (acc, v) => acc :+ v)(
    vec => {
      val sorted = vec.sorted
      val size = vec.size
      vec(size/2)
    }
  )

case class IntegrateOverAllContainersByTime() extends AggregationOverAllContainersByTime[Double, Double, Double](0D)(
  (acc, v: Double) => acc + v)(identity) {

  override def values(): Iterable[(Long, Double)] = {
    val res = super.values()
    if (res.isEmpty) return Iterable()

    val (firstTime, firstVal) = res.head
    res.scanLeft((firstTime, 0D)){ case ((prevTime, prevVal), (time, value)) =>
      val timeDelta = time - prevTime
      val valueByTime = value * timeDelta
      (time, prevVal + value)
    }.drop(1) // drop zero value
  }
}

case class TracesAggregation2(allowedPrefixes: Set[String],
                              minSampleRatio: Double,
                              maxDepth: Int) extends Aggregation2[Gauge, TraceNode] {

  private val root = new TraceNode("root")

  override def aggregate(g: Gauge): Unit = {
    val splits = g.label.split('|')
      .drop(1) // drop the thread name as we aggregate over all threads
      // drop until we match a custom prefix
      .dropWhile(m => allowedPrefixes.nonEmpty && !allowedPrefixes.exists(m.startsWith))

    if (splits.nonEmpty) {
      val samplesCount = g.value.toLong
      root.value.add(samplesCount)

      // filter out methods too deep in the call stack
      val filteredSplits = if (maxDepth > 0) splits.take(maxDepth) else splits

      filteredSplits.foldLeft(root){ (parent, method) =>
        val children = parent.children.getOrElseUpdate(method, new TraceNode(method, new MutableLong(g.timestamp)))
        if (children.firstTimestamp.getValue > g.timestamp) children.firstTimestamp.setValue(g.timestamp)
        children.value.add(samplesCount)
        children
      }
    }
  }

  override def values(): Iterable[TraceNode] = {
    val minSamples = (root.value.getValue * minSampleRatio).toLong
    if (minSamples > 1) pruneTooLittleSamples(root, minSamples)
    Some(root)
  }

  private def pruneTooLittleSamples(n: TraceNode, minSamples: Long): Unit = {
    n.children.foreach{
      case (key, node) if node.value.getValue < minSamples => n.children.remove(key)
      case (key, node) => pruneTooLittleSamples(node, minSamples)
    }
  }

  override def json(): JSONObject = values().head.json()
}


