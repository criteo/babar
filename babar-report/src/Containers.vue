<template>
  <div id="overview">

    <PlotTimeSeries title="Running containers"  yAxis="containers" :series="series.containers" />
    <hr>
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
