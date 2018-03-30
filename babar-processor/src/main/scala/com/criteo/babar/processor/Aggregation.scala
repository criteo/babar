package com.criteo.babar.processor

trait Aggregation[IN, ACC, OUT] {

  def zero: ACC

  def accumulate(value: IN): ACC

  def value: OUT

}
