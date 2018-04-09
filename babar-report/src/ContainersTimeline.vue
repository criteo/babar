<template>
  <div>

    <b-container fluid>
      <b-row>
          <b-col sm="8">
            <b-pagination :total-rows="totalRows" :per-page="perPageValue()" v-model="currentPage" />
          </b-col>
          <b-col sm="4" class="per-page-form">
            <label>Per page</label>
            <b-form-select :options="pageOptions" v-model="perPage" />
          </b-col>
      </b-row>
      <b-row>
        <b-col>
          <b-table small hover
            :items="containers" 
            class="containers-table" 
            :current-page="currentPage" 
            :per-page="perPageValue()" 
            :fields="fields">
            
            <template slot="container" slot-scope="data">
              <b-badge variant="secondary">{{data.value}}</b-badge>
            </template>
            <template slot="duration" slot-scope="data">
              {{data.value | formatDuration}}
            </template>
            <template slot="timeline" slot-scope="row">
              <div class="timeline" v-b-tooltip.hover :title="timelineTooltip(row.item.start, row.item.stop)" >
                <span :style="timelineMargins(row.item.timeline.left, row.item.timeline.right)" >
                </span>
              </div>
            </template>
          </b-table>
        </b-col>
      </b-row>
    </b-container>
    
  </div>
</template>

<script>
import Vue from "vue"
import BootstrapVue from "bootstrap-vue"
import _ from "lodash"
import StringUtils from "./stringUtils.js"
import DateUtils from './dateUtils.js'

export default {
  data() {
    const _minMax = minMax(window.data["containers timeline"])
    const containers = makeContainersItems(window.data["containers timeline"], _minMax[0], _minMax[1])
    return {
      fields: ["container", 'duration', 'timeline'],
      containers: containers,
      totalRows: containers.length,
      pageOptions: [ "20", "50", "100", "200", "All" ],
      currentPage: 1,
      perPage: "100",
    };
  },
  methods: {
    timelineMargins(left, right) {
      return "margin-left:" + left + "%;margin-right:" + right + "%;"
    },
    timelineTooltip(start, end) {
      return "started at " + start + "\nended at " + end
    },
    perPageValue() {
      return this.perPage == "All" ? Number.MAX_SAFE_INTEGER : Number(this.perPage)
    }
  }
};

function makeContainersItems(containers, globalStart, globalStop) {
  return _.map(containers, c => ({
    "container": c.container,
    "duration": c.stop - c.start,
    "timeline": makeTimeline(c, globalStart, globalStop),
    "start": DateUtils.formatTime(c.start),
    "stop": DateUtils.formatTime(c.stop)
  }))
}

function  makeTimeline(container, globalStart, globalStop) {
  const globalDuration = Math.max(0, globalStop - globalStart)
  const left = globalDuration > 0 ? (container.start - globalStart) / globalDuration * 100 : 0
  const right = globalDuration > 0 ? (globalStop - container.stop) / globalDuration * 100 : 0
  return {left: left, right: right}
}

function minMax(containers) {
  return _.reduce(containers,
      (minMaxTuple, v) => [
        Math.min(minMaxTuple[0], v.start),
        Math.max(minMaxTuple[1], v.stop)
      ], [containers[0].start, containers[0].stop])
}

</script>

<style lang="scss">

.containers-table {

  th {
    text-align: center;
  }

  td {
    padding: 1px 0;
  }

  td[aria-colindex="1"]  {
    width: 5%;
    font-size: 1.1em;
  }

  td[aria-colindex="2"]  {
    width: 5%;
    text-align: center;
  }

  td[aria-colindex="3"]  {
    width: 90%;
  }

  .timeline {
    background-color: #f5f5f5f5;
    border: 1px solid #cccccc;
    margin: 2px 0;

    span {
      display: block;
      height: 18px;
      margin-top: 0px;
      margin-bottom: 0px;
      background-color: #74B3DF;
    }
  }
}

.per-page-form {
  text-align: right;

  label {
    margin-right: 20px;
  }

  select {
    width: 80px;
  }
}

</style>
