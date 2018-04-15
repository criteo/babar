<template>
  <div id="overview">

    <PlotTimeSeries title="Containers"  yAxis="containers" :series="series.containers" />
    <hr>
    <ContainersTimeline />

  </div>
</template>

<script>
import Vue from 'vue'
import PlotTimeSeries from './PlotTimeSeries.vue'
import ContainersTimeline from './ContainersTimeline.vue'

export default {
  name: 'Babar-report',
  components: {
    PlotTimeSeries, ContainersTimeline
  },
  data() {
    return {
      series: {
        containers: [
          {
            name: "containers",
            time: window.data["containers"].time,
            values: window.data["containers"].values
          }
        ]
      }
    }
  }
}
</script>

<style lang="scss">
</style>
