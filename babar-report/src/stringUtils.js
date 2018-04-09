import _ from "lodash";

export default {
  commasToArray(string) {
    if (string === undefined) return [];
    return _.chain(string.trim().split(","))
      .map(p => p.trim())
      .filter(p => p != "")
      .value();
  },

  hashCode(str) {
    var hash = 0, i = 0, len = str.length;
    while ( i < len ) {
        hash  = ((hash << 5) - hash + str.charCodeAt(i++)) << 0;
    }
    return hash;
  },

  randomUUID() {
    return Math.random().toString(36).substring(2) + (new Date()).getTime().toString(36);
  },

  limit(str, length) {
    if (str.length > length) return str.substring(0, length)
    return str
  }
};
