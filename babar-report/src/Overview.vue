<template>
  <div id="overview">

    <PlotTimeSeries title="Containers" :series="series.containers" />
    <!--<PlotTimeSeries title="Memory" :series="series.memory" />-->

  </div>
</template>

<script>
import Vue from 'vue'
import PlotTimeSeries from './PlotTimeSeries.vue'

export default {
  name: 'Babar-report',
  components: {
    PlotTimeSeries
  },
  data() {
    return {
      series: {
        containers: [
          {
            name: "containers",
            time: window.metrics["containers"].time,
            values: window.metrics["containers"].values
          }
        ]/*,
        memory: [
          {
            name: "total reserved",
            time: window.metrics.time,
            values: window.metrics["total reserved"]
          },
          {
            name: "total RSS memory",
            time: window.metrics.time,
            values: window.metrics["total RSS memory"]
          },
          {
            name: "total used heap",
            time: window.metrics.time,
            values: window.metrics["total used heap"],
            stack: true
          },
          {
            name: "total used off-heap",
            time: window.metrics.time,
            values: window.metrics["total used off-heap"],
            stack: true
          }
        ]*/
      }
    }
  }
}
</script>

<style lang="scss">
</style>
