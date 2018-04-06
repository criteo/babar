<template>
  <div>

    <b-container class="text-center" fluid>
      <PlotTimeSeries title="Minor & Major median GC ratio" yAxis="%time" :series="series.minorMajor" />
      <PlotTimeSeries title="Max & Median GC ratio" yAxis="%time" :series="series.maxMedian" />
      <PlotTimeSeries title="Accumulated JVM CPU time and GC CPU time" yAxis="sec" :series="series.accumulatedCpuGc" />
    </b-container>

  </div>
</template>

<script>
import Vue from 'vue'
import PlotTimeSeries from './PlotTimeSeries.vue'
import $ from 'jquery'
import _ from 'lodash'

export default {
  components: {
    PlotTimeSeries
  },
  data() {
    return {
      large: $(window).width() >= 1000,
      tab: "total",
      series: {
        minorMajor: _.filter([
          _.assign({}, window.data["median major GC ratio"], { name: "median major GC ratio"}),
          _.assign({}, window.data["median minor GC ratio"], { name: "median minor GC ratio"})
        ], s => s.values),
        maxMedian: _.filter([
          _.assign({}, window.data["max GC ratio"], { name: "max GC ratio", color: "#dddddd"}),
          _.assign({}, window.data["median GC ratio"], { name: "median GC ratio"})
        ], s => s.values),
        accumulatedCpuGc: _.filter([
          _.assign({}, window.data["accumulated JVM CPU time"], { name: "accumulated JVM CPU time", color: "#dddddd"}),
          _.assign({}, window.data["accumulated GC CPU time"], { name: "accumulated GC CPU time"})
        ], s => s.values)
      }
    }
  }
}
</script>

<style lang="scss">
.switch-container {
  padding: 20px 0;
}
.memory-swicth {
  float: right;
}
</style>
