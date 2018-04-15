<template>
  <div id="overview">

    <template v-if="isProfilersUsed">
      <PlotTimeSeries title="Running containers"  yAxis="containers" :series="series.containers" />
      <div class="explanation">
        The graph above shows the number of running containers at any given time for you application.<br>
        Below you will find the list of all of the application containers, along with their duration and a visual represerntation of the time they have been running over
        total application run-time.
      </div>
      <ContainersTimeline />
    </template>

    <b-alert show variant="warning" v-if="!isProfilersUsed">
      No data has been found, make sure either the <strong>JVMProfiler</strong> or the <strong>ProcFSProfiler</strong> profilers have been used
      to profile your application.
    </b-alert>

  </div>
</template>

<script>
import Vue from 'vue'
import PlotTimeSeries from './PlotTimeSeries.vue'
import ContainersTimeline from './ContainersTimeline.vue'
import Constants from './constants.js'

export default {
  name: 'Babar-report',
  components: {
    PlotTimeSeries, ContainersTimeline
  },
  data() {
    return {
      isProfilersUsed: window.data["isJvmProfiler"] || window.data["isProcFSProfiler"],
      series: {
        containers: [
          _.assign({}, window.data["containers"], {name: "containers", color: Constants.DARK_RED})
        ]
      }
    }
  }
}
</script>

<style lang="scss">
</style>
