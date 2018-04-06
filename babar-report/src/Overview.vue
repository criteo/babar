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
            time: window.data["containers"].time,
            values: window.data["containers"].values
          }
        ]/*,
        memory: [
          {
            name: "total reserved",
            time: window.data.time,
            values: window.data["total reserved"]
          },
          {
            name: "total RSS memory",
            time: window.data.time,
            values: window.data["total RSS memory"]
          },
          {
            name: "total used heap",
            time: window.data.time,
            values: window.data["total used heap"],
            stack: true
          },
          {
            name: "total used off-heap",
            time: window.data.time,
            values: window.data["total used off-heap"],
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
