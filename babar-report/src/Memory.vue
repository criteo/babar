<template>
  <div>

    <b-alert show variant="warning" v-if="!isJvmProfilerUsed">
      No data has been found, make sure the <strong>JVMProfiler</strong> has been used
      to profile your application.
    </b-alert>

    <b-alert show variant="warning" v-if="!isProcFSProfilerUsed">
      No data has been found for the <strong>ProcFSProfiler</strong>, to get additional information about memory, make sure it is used
      to profile your application.
    </b-alert>

    <template v-if="isJvmProfilerUsed">

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

      <b-container fluid v-if="tab=='total'" key="total">
        <PlotTimeSeries title="Total used memory" yAxis="MB" :series="series.totalUsed" />
        <div class="explanation">
          This graph shows the total memory used by all of your application's running containers at any given time. <br>
          <strong>Reserved memory</strong> is the amount of memory reserved on the infrastructure.<br>
          <strong>RSS memory</strong> is the size of the memory pages that are loaded in the physical memory (RAM) for your containers process-tree (inclusing non JVM programs).<br>
          <strong>Anonymous page bytes</strong> is the size of the memory pages that not mapped to a file on a block device (called "anonymous"), excluding read-only shared pages (r--s or r-xs). 
          This is the memory accounted for by YARN if <samp class="conf-value">yarn.nodemanager.container-monitor.procfs-tree.smaps-based-rss.enabled</samp> 
          is <samp class="conf-value">true</samp><br>
        </div>
        <PlotTimeSeries title="Total committed memory" yAxis="MB" :series="series.totalCommitted" />
        <div class="explanation">
          This graph shows the total memory used by all of your application's running containers at any given time, including the memory committed by the JVM. <br>
          The <strong>committed heap-memory</strong> is the amount of memory proactively reserved by the JVM so that it can grow its heap, however <strong>not all of the committed memory is used</strong>.<br>
          Similarly, the <strong>committed off-heap memory</strong> is larger than the actual used off-heap memory.<br>
          If a large amount of the committed memory is not used, you could reduce the maximum heap size (using the java <samp class="conf-value">-Xmx</samp> option) to set an upper limit
          to the committed heap memory.
        </div>
        <PlotTimeSeries title="Total accumulated memory" yAxis="MB*sec" :series="series.accumulated" />
        <div class="explanation">
          This graph shows the accumulated amount of physical memory used by your application since its start. <br>
          The value in <strong>MB*sec</strong> is the integral of the memory used (MB) over the time it was used (sec).
        </div>
      </b-container>

      <b-container class="text-center" fluid v-if="tab=='max'" key="max">
        <PlotTimeSeries title="Max used memory for any container" yAxis="MB" :series="series.maxUsed" />
        <div class="explanation">
          This graph shows the maximum memory used and reserved by any container.<br>
          <strong>Reserved memory</strong> is the amount of memory reserved on the host, whether used or not.<br>
          <strong>RSS memory</strong> is the size of the memory pages that are loaded in the physical memory (RAM) for the container's process-tree (inclusing non JVM programs).<br>
          <strong>Anonymous page bytes</strong> is the size of the memory pages that not mapped to a file on a block device (called "anonymous"), excluding read-only shared pages (r--s or r-xs). 
          This is the memory accounted for by YARN if <samp class="conf-value">yarn.nodemanager.container-monitor.procfs-tree.smaps-based-rss.enabled</samp> 
          is <samp class="conf-value">true</samp><br>
        </div>
        <PlotTimeSeries title="Max committed memory for any container" yAxis="MB" :series="series.maxCommitted" />
        <div class="explanation">
          This graph shows the maximum memory committed and reserved by any container.<br>
          The <strong>committed heap-memory</strong> is the amount of memory proactively reserved by the JVM so that it can grow its heap, however <strong>not all of the committed memory is used</strong>.<br>
          Similarly, the <strong>committed off-heap memory</strong> is larger than the actual used off-heap memory.<br>
          If a large amount of the committed memory is not used, you could reduce the maximum heap size (using the java <samp class="conf-value">-Xmx</samp> option) to set an upper limit
          to the committed heap memory.
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
      isJvmProfilerUsed: window.data["isJvmProfiler"],
      isProcFSProfilerUsed: window.data["isProcFSProfiler"],
      tab: "total",
      series: {
        totalUsed: _.filter([
          _.assign({}, window.data["total reserved"], { name: "total reserved", color: Constants.DARK_RED }),
          _.assign({}, window.data["total RSS memory"], { name: "total RSS memory", color: Constants.BLUE }),
          _.assign({}, window.data["total anonymous page bytes"], { name: "total anonymous page bytes", color: Constants.DARK_BLUE }),
          _.assign({}, window.data["total used heap"], { name: "total used JVM heap", color: Constants.ORANGE, stack: true }),
          _.assign({}, window.data["total used off-heap"], { name: "total used JVM off-heap", color: Constants.YELLOW, stack: true })
        ], s => s.values),
        maxUsed: _.filter([
          _.assign({}, window.data["max reserved"], { name: "max reserved", color: Constants.DARK_RED }),
          _.assign({}, window.data["max RSS memory"], { name: "max RSS memory", color: Constants.BLUE }),
          _.assign({}, window.data["max anonymous page bytes"], { name: "max anonymous page bytes", color: Constants.DARK_BLUE }),
          _.assign({}, window.data["max used heap"], { name: "max used JVM heap", color: Constants.ORANGE, stack: true }),
          _.assign({}, window.data["max used off-heap"], { name: "max used JVM off-heap", color: Constants.YELLOW, stack: true })
        ], s => s.values),
        totalCommitted: _.filter([
          _.assign({}, window.data["total reserved"], { name: "total reserved", color: Constants.DARK_RED }),
          _.assign({}, window.data["total RSS memory"], { name: "total RSS memory", color: Constants.BLUE }),
          _.assign({}, window.data["total anonymous page bytes"], { name: "total anonymous page bytes", color: Constants.DARK_BLUE }),
          _.assign({}, window.data["total committed heap"], { name: "total committed JVM heap", color: Constants.ORANGE, stack: true }),
          _.assign({}, window.data["total committed off-heap"], { name: "total committed JVM off-heap", color: Constants.YELLOW, stack: true })
        ], s => s.values),
        maxCommitted: _.filter([
          _.assign({}, window.data["max reserved"], { name: "max reserved", color: Constants.DARK_RED }),
          _.assign({}, window.data["max RSS memory"], { name: "max RSS memory", color: Constants.BLUE }),
          _.assign({}, window.data["max anonymous page bytes"], { name: "max anonymous page bytes", color: Constants.DARK_BLUE }),
          _.assign({}, window.data["max committed heap"], { name: "max committed JVM heap", color: Constants.ORANGE, stack: true }),
          _.assign({}, window.data["max committed off-heap"], { name: "max committed JVM off-heap", color: Constants.YELLOW, stack: true })
        ], s => s.values),
        accumulated: _.filter([
          _.assign({}, window.data["accumulated reserved"], { name: "accumulated reserved", color: Constants.DARK_RED }),
          _.assign({}, window.data["accumulated RSS memory"], { name: "accumulated RSS memory", color: Constants.BLUE }),
          _.assign({}, window.data["accumulated anonymous page bytes"], { name: "accumulated anonymous page bytes", color: Constants.DARK_BLUE }),
          _.assign({}, window.data["accumulated used heap"], { name: "accumulated used JVM heap", color: Constants.ORANGE, stack: true }),
          _.assign({}, window.data["accumulated used off-heap"], { name: "accumulated used JVM off-heap", color: Constants.YELLOW, stack: true })
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
