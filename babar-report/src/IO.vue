<template>
  <div>
    
    <b-container fluid>
      <PlotTimeSeries title="Process tree bytes read / sec" yAxis="bytes/sec" :series="series.bytesRead" />
      <PlotTimeSeries title="Process tree Accumulated bytes read" yAxis="bytes" :series="series.accumulatedBytesRead" />
      <PlotTimeSeries title="Process tree bytes written / sec" yAxis="bytes/sec" :series="series.bytesWrite" />
      <PlotTimeSeries title="Process tree accumulated bytes written" yAxis="bytes" :series="series.accumulatedBytesWrite" />
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
        bytesRead: _.filter([
          _.assign({}, window.data["max proc read bytes / sec"], { name: "max bytes read/sec", color: Constants.GREY}),
          _.assign({}, window.data["median proc read bytes / sec"], { name: "median bytes read/sec", color: Constants.DARK_BLUE}),
          _.assign({}, window.data["median proc disk read bytes / sec"], { name: "median disk bytes read/sec", color: Constants.LIGHT_BLUE})
        ], s => s.values),
        bytesWrite: _.filter([
          _.assign({}, window.data["max proc write bytes / sec"], { name: "max bytes written/sec", color: Constants.GREY}),
          _.assign({}, window.data["median proc write bytes / sec"], { name: "median bytes written/sec", color: Constants.DARK_RED}),
          _.assign({}, window.data["median proc disk write bytes / sec"], { name: "median disk bytes written/sec", color: Constants.ORANGE})
        ], s => s.values),
        accumulatedBytesRead: _.filter([
          _.assign({}, window.data["accumulated proc read bytes"], { name: "bytes read", color: Constants.DARK_BLUE}),
          _.assign({}, window.data["accumulated proc disk read bytes"], { name: "disk bytes read", color: Constants.LIGHT_BLUE})
        ], s => s.values),
        accumulatedBytesWrite: _.filter([
          _.assign({}, window.data["accumulated proc write bytes"], { name: "bytes write", color: Constants.DARK_RED}),
          _.assign({}, window.data["accumulated proc disk write bytes"], { name: "disk bytes write", color: Constants.ORANGE})
        ], s => s.values)
      }
    }
  }
}
</script>

<style lang="scss">
</style>
