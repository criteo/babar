<template>
  <div>

    <b-container class="text-center switch-container">
      <b-card bg-variant="light">
        <b-button-group class="memory-swicth">
          <b-button :variant="tab == 'total' ? 'success' : 'outline-success'" @click="tab='total'" id="total-btn">Total memory</b-button>
          <b-button :variant="tab == 'max' ? 'success' : 'outline-success'" @click="tab='max'" id="max-btn">Max memory</b-button>
        </b-button-group>
        <p class="card-text text-left">
            <strong>Total memory</strong> shows the sum of the memory usage over all containers
            <br />
            <strong>Max memory</strong> shows the maximum memory usage for any container
        </p>
      </b-card>
    </b-container>

    <b-container class="text-center" fluid v-if="tab=='total'" key="total">
      <PlotTimeSeries title="Total used memory" yAxis="MB" :series="series.totalUsed" />
      <PlotTimeSeries title="Total committed memory" yAxis="MB" :series="series.totalCommitted" />
      <PlotTimeSeries title="Total accumulated memory" yAxis="MB*sec" :series="series.accumulated" />
    </b-container>

    <b-container class="text-center" fluid v-if="tab=='max'" key="max">
      <PlotTimeSeries title="Max used memory for any container" yAxis="MB" :series="series.maxUsed" />
      <PlotTimeSeries title="Max committed memory for any container" yAxis="MB" :series="series.maxCommitted" />
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
        totalUsed: _.filter([
          _.assign({}, window.data["total reserved"], { name: "total reserved", color: Constants.DARK_RED }),
          _.assign({}, window.data["total RSS memory"], { name: "total RSS memory", color: Constants.DARK_BLUE }),
          _.assign({}, window.data["total used heap"], { name: "total used heap", color: Constants.LIGHT_BLUE, stack: true }),
          _.assign({}, window.data["total used off-heap"], { name: "total used off-heap", color: Constants.ORANGE, stack: true })
        ], s => s.values),
        maxUsed: _.filter([
          _.assign({}, window.data["max reserved"], { name: "total reserved", color: Constants.DARK_RED }),
          _.assign({}, window.data["max RSS memory"], { name: "total RSS memory", color: Constants.DARK_BLUE }),
          _.assign({}, window.data["max used heap"], { name: "total used heap", color: Constants.LIGHT_BLUE, stack: true }),
          _.assign({}, window.data["max used off-heap"], { name: "total used off-heap", color: Constants.ORANGE, stack: true })
        ], s => s.values),
        totalCommitted: _.filter([
          _.assign({}, window.data["total reserved"], { name: "total reserved", color: Constants.DARK_RED }),
          _.assign({}, window.data["total RSS memory"], { name: "total RSS memory", color: Constants.DARK_BLUE }),
          _.assign({}, window.data["total committed heap"], { name: "total committed heap", color: Constants.LIGHT_BLUE, stack: true }),
          _.assign({}, window.data["total committed off-heap"], { name: "total committed off-heap", color: Constants.ORANGE, stack: true })
        ], s => s.values),
        maxCommitted: _.filter([
          _.assign({}, window.data["max reserved"], { name: "total reserved", color: Constants.DARK_RED }),
          _.assign({}, window.data["max RSS memory"], { name: "total RSS memory", color: Constants.DARK_BLUE }),
          _.assign({}, window.data["max committed heap"], { name: "total committed heap", color: Constants.LIGHT_BLUE, stack: true }),
          _.assign({}, window.data["max committed off-heap"], { name: "total committed off-heap", color: Constants.ORANGE, stack: true })
        ], s => s.values),
        accumulated: _.filter([
          _.assign({}, window.data["accumulated reserved"], { name: "accumulated reserved", color: Constants.DARK_RED }),
          _.assign({}, window.data["accumulated RSS memory"], { name: "accumulated RSS memory", color: Constants.DARK_BLUE }),
          _.assign({}, window.data["accumulated used heap"], { name: "accumulated used heap", color: Constants.LIGHT_BLUE, stack: true }),
          _.assign({}, window.data["accumulated used off-heap"], { name: "accumulated used off-heap", color: Constants.ORANGE, stack: true })
        ], s => s.values)
      }
    }
  }
}
</script>

<style lang="scss">
.switch-container {
  padding: 20px 0;
}
.memory-swicth {
  float: right;
}
</style>
