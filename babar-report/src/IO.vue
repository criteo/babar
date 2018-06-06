<template>
  <div>

    <b-alert show variant="warning" v-if="!isProcFSProfilerUsed">
      No data has been found for the IO usage from the proc filesystem, make sure the <strong>ProcFSProfiler</strong> has been used
      to profile your application.
    </b-alert>

    <template v-if="isProcFSProfilerUsed">
    
      <b-container fluid>
        <PlotTimeSeries title="Network bytes received/transmitted / sec" yAxis="bytes/sec" :series="series.netIO" />
        <div class="explanation">
          This graph shows the total number of bytes of data transmitted or received by all interfaces as reported by the <kbd>/prod/net/dev</kbd> file.<br>
        </div>      
        <PlotTimeSeries title="Process tree peak bytes read / sec" yAxis="bytes/sec" :series="series.bytesRead" />
        <div class="explanation">
          This graph shows the peak bandwidth used to read data from disk I/O by the entire process-tree as reported by the <kbd>/prod/[pid]/io</kbd> file.<br>
          <strong>Bytes read/sec</strong> show the bandwidth for all read operations, while <strong>disk bytes read/sec</strong> only shows the bandwidth for 
          I/O operations that acutally read data from the block storage layer (i.e. excluding pagecache).
        </div>      
        <PlotTimeSeries title="Process tree Accumulated bytes read" yAxis="bytes" :series="series.accumulatedBytesRead" />
        <div class="explanation">
          This graph shows the accumulated bytes read from disk I/O since the start of the application by the entire process-tree as reported by the <kbd>/prod/[pid]/io</kbd>,
          as well the the amound of bytes actually read from the block-storage layer (i.e. excluding pagecache).
        </div>      
        <PlotTimeSeries title="Process tree peak bytes written / sec" yAxis="bytes/sec" :series="series.bytesWrite" />
        <div class="explanation">
          This graph shows the peak bandwidth used to write data to I/O by the entire process-tree as reported by the <kbd>/prod/[pid]/io</kbd> file.<br>
          <strong>Bytes read/sec</strong> show the bandwidth for all write operations, while <strong>disk bytes written/sec</strong> only shows the bandwidth for 
          I/O operations that acutally wrote data to a block storage layer (i.e. excluding pagecache).
        </div>      
        <PlotTimeSeries title="Process tree accumulated bytes written" yAxis="bytes" :series="series.accumulatedBytesWrite" />
        <div class="explanation">
          This graph shows the accumulated bytes written from I/O since the start of the application by the entire process-tree as reported by the <kbd>/prod/[pid]/io</kbd>,
          as well the the amound of bytes actually written from the block-storage layer (i.e. excluding pagecache).
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
      tab: "total",
      isProcFSProfilerUsed: window.data["isProcFSProfiler"],
      series: {
        netIO: _.filter([
          _.assign({}, window.data["proc net rx bytes / sec"], { name: "bytes received/sec", color: Constants.YELLOW, opacity: 0.5}),
          _.assign({}, window.data["proc net tx bytes / sec"], { name: "bytes transmitted/sec", color: Constants.BLUE, opacity: 0.5})
        ], s => s.values),
        bytesRead: _.filter([
          _.assign({}, window.data["max proc peak read bytes / sec"], { name: "max bytes read/sec", color: Constants.GREY}),
          _.assign({}, window.data["median proc peak read bytes / sec"], { name: "median bytes read/sec", color: Constants.DARK_BLUE}),
          _.assign({}, window.data["median proc peak disk read bytes / sec"], { name: "median disk bytes read/sec", color: Constants.LIGHT_BLUE})
        ], s => s.values),
        bytesWrite: _.filter([
          _.assign({}, window.data["max proc peak write bytes / sec"], { name: "max bytes written/sec", color: Constants.GREY}),
          _.assign({}, window.data["median proc peak write bytes / sec"], { name: "median bytes written/sec", color: Constants.DARK_RED}),
          _.assign({}, window.data["median proc peak disk write bytes / sec"], { name: "median disk bytes written/sec", color: Constants.ORANGE})
        ], s => s.values),
        accumulatedBytesRead: _.filter([
          _.assign({}, window.data["accumulated proc read bytes"], { name: "bytes read", color: Constants.DARK_BLUE}),
          _.assign({}, window.data["accumulated proc disk read bytes"], { name: "disk bytes read", color: Constants.LIGHT_BLUE})
        ], s => s.values),
        accumulatedBytesWrite: _.filter([
          _.assign({}, window.data["accumulated proc write bytes"], { name: "bytes write", color: Constants.DARK_RED}),
          _.assign({}, window.data["accumulated proc disk write bytes"], { name: "disk bytes write", color: Constants.ORANGE})
        ], s => s.values)
      }
    }
  }
}
</script>

<style lang="scss">
</style>
