<template>
  <div id="overview">

    <PlotTimeSeries title="Running containers"  yAxis="containers" :series="series.containers" />

    <div class="explanation">
      The graph above shows the number of running containers at any given time for you application.<br>
      Below you will find the list of all of the application containers, along with their duration and a visual represerntation of the time they have been running over
      total application run-time.
    </div>

    <ContainersTimeline />

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
