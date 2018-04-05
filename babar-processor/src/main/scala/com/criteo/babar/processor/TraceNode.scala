package com.criteo.babar.processor

import org.apache.commons.lang3.mutable.MutableLong

import scala.collection.mutable
import scala.util.parsing.json.{JSONArray, JSONObject}

class TraceNode(val name: String,
                val firstTimestamp: MutableLong = new MutableLong(0L),
                val value: MutableLong = new MutableLong(0L),
                val children: mutable.Map[String, TraceNode] = mutable.Map.empty) {

  def json(): JSONObject = {
    JSONObject(Map(
      "name" -> name,
      "value" -> value.getValue,
      "children" -> JSONArray(children.values.toList.sortBy(_.firstTimestamp.getValue).map(_.json()))
    ))
  }
}
