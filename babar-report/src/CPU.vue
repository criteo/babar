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
            <strong>ProcFS</strong> shows the memory usage as reported by the <kbd>/proc</kbd> filesystem using the <i>ProcFSProfiler</i>.
        </p>
      </b-card>
    </b-container>

    <b-container fluid v-if="tab=='jvm'" key="jvm">
      <PlotTimeSeries title="JVM CPU usage" yAxis="usage" yMax="1" :series="series.jvm" />
      <div class="explanation">
        This graph shows the median and max <strong>CPU usage as reported by the JVM instrumentation</strong> over all the containers.<br>
        It only reports the JVM CPU usage and will ignore children processes spawned by the java application.<br>
      </div>
      <PlotTimeSeries title="Host CPU usage" yAxis="usage" yMax="1" :series="series.jvmHost" />
      <div class="explanation">
        This graph shows the median and max <strong>host CPU usage as reported by the JVM instrumentation</strong> over all the containers.<br>
        The host CPU usage is the CPU usage of the machine on which the containers are running.<br>
        This graph can be useful to understand why some containers take longer than others if they are scheduled on very busy hosts.
      </div>
      <PlotTimeSeries title="Accumulated JVM CPU seconds" yAxis="sec" :series="series.jvmAccumulated" />
      <div class="explanation">
        This graph show the total amount of CPU time used on all containers since the start of the application.
      </div>
    </b-container>

    <b-container fluid v-if="tab=='procfs'" key="procfs">
      <PlotTimeSeries title="User & Kernel process tree CPU load" yAxis="load" yMax="1" :series="series.procModes" />
      <div class="explanation">
        This graph shows the median and max <strong>CPU load as reported by the <kbd>/proc/[pid]/stat</kbd> file</strong> over all the containers.<br>
        It shows the split of the load in <strong>user</strong> and <strong>kernel</strong> modes. 
        Time spent in kernel mode include, for instance, part of some I/O operations such as disk access.<br>
        It takes into account the total CPU load of all children processes spawned by the java application (e.g. python processes).<br>
      </div>
      <PlotTimeSeries title="Host CPU load" yAxis="load" yMax="1" :series="series.procHost" />
      <div class="explanation">
        This graph shows the median and max <strong>host CPU load as reported by the <kbd>/proc/stat</kbd> file</strong> over all the containers.
      </div>
      <PlotTimeSeries title="Accumulated process tree CPU seconds" yAxis="sec" :series="series.procAccumulated" />
      <div class="explanation">
        This graph shows the total amount of CPU time used on all containers since the start of the application as reported by the <kbd>/proc/stat</kbd> file.
      </div>
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
        procModes: _.filter([
          _.assign({}, window.data["max proc tree CPU load"], { name: "max load", color: Constants.GREY }),
          _.assign({}, window.data["median proc tree user CPU load"], { name: "median user mode CPU load", color: Constants.DARK_RED, stack: true }),
          _.assign({}, window.data["median proc tree kernel CPU load"], { name: "median kernel mode CPU load", color: Constants.ORANGE, stack: true })
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
