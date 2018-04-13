<template>
  <div id="app">

    <b-navbar toggleable type="light" variant="light">
      <b-navbar-toggle target="nav_text_collapse"></b-navbar-toggle>
      <b-navbar-brand>Babar</b-navbar-brand>
      <b-collapse is-nav id="nav_text_collapse">
        <b-navbar-nav>
          <b-nav-item href="#" @click="tab='containers'">Containers</b-nav-item>
          <b-nav-item href="#" @click="tab='memory'">Memory</b-nav-item>
          <b-nav-item href="#" @click="tab='cpu'">CPU</b-nav-item>
          <b-nav-item href="#" @click="tab='gc'">GC</b-nav-item>
          <b-nav-item href="#" @click="tab='traces'">Traces</b-nav-item>
        </b-navbar-nav>
      </b-collapse>
    </b-navbar>

   <b-container fluid id="content">

      <Overview v-if="tab=='containers'"/>
      <Memory v-if="tab=='memory'" />
      <CPU v-if="tab=='cpu'" />
      <GC v-if="tab=='gc'" />
      <Traces v-if="tab == 'traces'"/>

    </b-container>

  </div>
</template>

<script>
import Vue from 'vue'
import BootstrapVue from 'bootstrap-vue'
import Overview from './Overview.vue'
import Traces from './Traces.vue'
import Memory from './Memory.vue'
import CPU from './CPU.vue'
import GC from './GC.vue'

import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'

Vue.use(BootstrapVue);

export default {
  name: 'Babar-report',
  components: {
    Overview, Traces, Memory, GC, CPU
  },
  data() {
    return {
      tab: "containers"
    }
  }
}
</script>

<style lang="scss">
#app {
  font-size: 0.9em;
}

.navbar .navbar-brand {
  font-size: 1.25em;
}

#content {
  padding-top: 12px;
}
</style>
