<template>
  <div>

    <b-container class="text-center switch-container">
      <b-card bg-variant="light">
        <b-button-group class="cpu-swicth">
          <b-button :variant="tab == 'jvm' ? 'success' : 'outline-success'" @click="tab='jvm'" id="total-btn">JVM</b-button>
          <b-button :variant="tab == 'procfs' ? 'success' : 'outline-success'" @click="tab='procfs'" id="max-btn">ProcFS</b-button>
        </b-button-group>
        <p class="card-text text-left">
            <strong>JVM</strong> shows the memory usage as reported by the JVM instrumentation using the <i>JVMProfiler</i>.
            <br />
            <strong>ProcFS</strong> shows the memory usage as reported by the proc filesystem (/proc) using the <i>ProcFSProfiler</i>.
        </p>
      </b-card>
    </b-container>

    <b-container fluid v-if="tab=='jvm'" key="jvm">
      <PlotTimeSeries title="JVM CPU usage" yAxis="usage" yMax="1" :series="series.jvm" />
      <PlotTimeSeries title="Host CPU usage" yAxis="usage" yMax="1" :series="series.jvmHost" />
       <PlotTimeSeries title="Accumulated JVM CPU seconds" yAxis="sec" :series="series.jvmAccumulated" />
    </b-container>

    <b-container fluid v-if="tab=='procfs'" key="procfs">
      <PlotTimeSeries title="Process tree CPU load" yAxis="load" yMax="1" :series="series.proc" />
      <PlotTimeSeries title="User & Kernel process tree CPU load" yAxis="load" yMax="1" :series="series.procModes" />
      <PlotTimeSeries title="Host CPU load" yAxis="load" yMax="1" :series="series.procHost" />
       <PlotTimeSeries title="Accumulated process tree CPU seconds" yAxis="sec" :series="series.procAccumulated" />
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
      tab: "jvm",
      series: {
        jvm: _.filter([
          _.assign({}, window.data["max JVM CPU load"], { name: "max load", color: Constants.GREY }),
          _.assign({}, window.data["median JVM CPU load"], { name: "median load", color: Constants.DARK_RED })
        ], s => s.values),
        jvmHost: _.filter([
          _.assign({}, window.data["max JVM host CPU load"], { name: "max load", color: Constants.GREY }),
          _.assign({}, window.data["median JVM host CPU load"], { name: "median load", color: Constants.DARK_BLUE })
        ], s => s.values),
        jvmAccumulated: _.filter([
          _.assign({}, window.data["accumulated JVM CPU time"], { name: "JVM CPU time", color: Constants.DRAK_RED })
        ], s => s.values),
        proc: _.filter([
          _.assign({}, window.data["max proc tree CPU load"], { name: "max load", color: Constants.GREY }),
          _.assign({}, window.data["median proc tree CPU load"], { name: "median load", color: Constants.DARK_RED })
        ], s => s.values),
        procModes: _.filter([
          _.assign({}, window.data["median proc tree user CPU load"], { name: "median user mode CPU load", color: Constants.DARK_BLUE, stack: true }),
          _.assign({}, window.data["median proc tree kernel CPU load"], { name: "median kernel mode CPU load", color: Constants.LIGHT_BLUE, stack: true })
        ], s => s.values),
        procHost: _.filter([
          _.assign({}, window.data["max proc host CPU load"], { name: "max load", color: Constants.GREY }),
          _.assign({}, window.data["median proc host CPU load"], { name: "median load", color: Constants.DARK_BLUE })
        ], s => s.values),
        procAccumulated: _.filter([
          _.assign({}, window.data["accumulated proc tree CPU time"], { name: "process tree CPU time", color: Constants.DRAK_RED })
        ], s => s.values),
      }
    }
  }
}
</script>

<style lang="scss">
.switch-container {
  padding: 20px 0;
}
.cpu-swicth {
  float: right;
}
</style>
