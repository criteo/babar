import Vue from "vue"
import DateUtils from './dateUtils.js'

// add fitler to format duration
Vue.filter('formatDuration', DateUtils.formatDuration)

export default {}