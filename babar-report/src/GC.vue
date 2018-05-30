<template>
  <div>

    <b-alert show variant="warning" v-if="!isJvmProfilerUsed">
        No data has been found for the JVM GC usage, make sure the <strong>JVMProfiler</strong> has been used
        to profile your application.
      </b-alert>

    <template v-if="isJvmProfilerUsed">

      <b-container class="text-center" fluid>
        <PlotTimeSeries title="Minor & Major median GC ratio" yAxis="%time" yMax="1" :series="series.minorMajor" />
        <div class="explanation">
          This graph shows the median ratio of wall-clock time spent doing minor and major garbage collections in the JVMs over all containers.<br>
          <strong>Minor GC</strong> only clean the young generation, while <strong>Major GC</strong> cleans both the young and old ones. The major GC 
          should be much less frequent that the minor one, otherwise it could indicate that too many short lived-objects are promototed to the old generation.
          If this is the case, you may want to resize the generations and make sure that no humongous object uses most of the old generation (which could 
          trigger frequent major GC).<br>
          You can tune the size of the generations either by specifying the <samp class="conf-value">-XX:NewRatio</samp> (integer value only) parameter or with the 
          <samp class="conf-value">-XX:NewSize</samp> and <samp class="conf-value">-XX:MaxNewSize</samp> parameters for finer-grained tuning.
        </div>
        <PlotTimeSeries title="Accumulated JVM CPU time and GC CPU time" yAxis="sec" :series="series.accumulatedCpuGc" />
        <div class="explanation">
          This graph shows the accumulated CPU time spent doing garbage collection since the start of the application.<br>
          This value is an estimation, computed by summing, for all time periods, the GC ratio (% time spent doing GC) multiplied by the JVM CPU time.
        </div>
      </b-container>

    </template>

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
      isJvmProfilerUsed: window.data["isJvmProfiler"],
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
