<template>
  <div>

    <b-container class="text-center" fluid>
      <PlotTimeSeries title="Minor & Major median GC ratio" yAxis="%time" yMax="1" :series="series.minorMajor" />
      <PlotTimeSeries title="Accumulated JVM CPU time and GC CPU time" yAxis="sec" :series="series.accumulatedCpuGc" />
    </b-container>

  </div>
</template>

<script>
import Vue from 'vue'
import PlotTimeSeries from './PlotTimeSeries.vue'
import _ from 'lodash'
import Constants from './constants.js'

export default {
  components: {
    PlotTimeSeries
  },
  data() {
    return {
      tab: "total",
      series: {
        minorMajor: _.filter([
          _.assign({}, window.data["max GC ratio"], { name: "max GC ratio", color: Constants.GREY}),
          _.assign({}, window.data["median major GC ratio"], { name: "median major GC ratio", color: Constants.DARK_BLUE, stack: true}),
          _.assign({}, window.data["median minor GC ratio"], { name: "median minor GC ratio", color: Constants.LIGHT_BLUE, stack: true})
        ], s => s.values),
        accumulatedCpuGc: _.filter([
          _.assign({}, window.data["accumulated JVM CPU time"], { name: "accumulated JVM CPU time", color: Constants.GREY}),
          _.assign({}, window.data["accumulated GC CPU time"], { name: "accumulated GC CPU time", color: Constants.DARK_RED})
        ], s => s.values)
      }
    }
  }
}
</script>

<style lang="scss">
</style>
