import moment from "moment";
import momentDurationFormatSetup from "moment-duration-format"
momentDurationFormatSetup(moment)

export default {
  formatDuration(durationMs) {
    return moment.duration(durationMs, 'milliseconds').format()
  },

  formatDate(timestampMs) {
    return moment(timestampMs).format('YYYY-MM-DD HH:mm:ss')
  },

  formatTime(timestampMs) {
    return moment(timestampMs).format('HH:mm:ss')
  }
};
