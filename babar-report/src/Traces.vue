<template>
    <div>
        <div>
            <b-form v-on:submit.prevent>
                <b-container fluid>

                    <b-row>
                        <b-col lg="6">
                            <b-form-group id="whitelistGroup" label="Whitelisted method-prefixes (comma-spearated)" label-for="whitelist">
                                <b-input-group>
                                    <b-form-input id="whitelist" type="text" v-model="whitelist" placeholder="eg: org.apache.hadoop.mapred,org.apache.spark" @change="updateTree">
                                    </b-form-input>
                                </b-input-group>
                            </b-form-group>
                        </b-col>
                        <b-col lg="6">
                            <b-form-group id="blacklistGroup" label="Blacklisted method-prefixes (comma-spearated)" label-for="blacklist">
                                <b-input-group>
                                    <b-form-input id="blacklist" type="text" v-model="blacklist" placeholder="eg: org.apache.hadoop.net.unix,sun.nio.ch.EPoll" @change="updateTree">
                                    </b-form-input>
                                </b-input-group>
                            </b-form-group>
                        </b-col>
                    </b-row>

                    <b-row>
                        <b-col lg="12">
                            <b-form-group id="traceSearchGroup" label="Search for method-prefixes (commas-spearated)" label-for="traceSearch">
                                <b-input-group>
                                    <b-form-input id="traceSearch" type="text" v-model="search" placeholder="eg: org.project.MyClass.myMethod" @change="updateTree">
                                    </b-form-input>
                                </b-input-group>
                            </b-form-group>

                            <b-table striped hover :items="searchResult"></b-table>
                        </b-col>
                    </b-row>

                </b-container>

            </b-form>
        </div>

        <div id="traces" style="width: 100%; height: 100%"></div>
    </div>
</template>

<script>
import Vue from 'vue'
import BootstrapVue from 'bootstrap-vue'
import $ from 'jquery'
import _ from 'lodash'
import * as _d3 from "d3"
import * as d3Tip from "d3-tip"
import { flamegraph as d3FlameGraph } from "d3-flame-graph"
import 'd3-flame-graph/dist/d3-flamegraph.css'
import StringUtils from './stringUtils'

const d3 = _.assign({}, _d3, { tip: d3Tip, flameGraph: d3FlameGraph })

const HIGHLIGHT_COLOR = "rgb(255, 255, 0)"

export default {
    props: {
    },
    data() {
        return {
            whitelist: "",
            blacklist: "",
            search: "",
            tree: {},
            searchResult: []
        }
    },
    mounted() {
        this.updateTree()
    },
    destroyed() {
        this.clean()
    },
    methods: {
        refresh() {
            const tip = d3.tip()
                .attr('class', 'd3-flame-graph-tip')
                .html(node => node.data.name + " (samples: " + node.value + ", " + ((node.x1 - node.x0) * 100).toFixed(1) + "%)")
                .direction(d => {
                    const upper = d.y0 >= 0.5
                    const left = d.x0 <= 0.5
                    const right = d.x1 >= 0.5

                    if (upper) {
                        if (left && right) return 's'
                        else if (left) return 'e'
                        else if (right) return 'w'
                    }
                    else {
                        if (left && right) return 'n'
                        else if (left) return 'e'
                        else if (right) return 'w'
                    }
                })

            this.flamegraph = d3.flameGraph()
                .width(0.98 * $("#traces").innerWidth())
                .tooltip(tip)
                .minFrameSize(4)
                .color(d => d.data.highlight == true ? HIGHLIGHT_COLOR : colorForNode(d.data))
                .inverted(true)

            d3.select("#traces")
                .datum(this.tree)
                .call(this.flamegraph)
        },
        clean() {
            if (this.flamegraph) {
                d3.select("#traces").selectAll("*").remove()
                // workaround to have tips correctly deleted
                $(".d3-flame-graph-tip").remove()
                this.flamegraph = undefined
            }
        },
        updateTree() {
            this.clean()
            const { tree, searchResult } = filterAndSearchTree(window.data["traces"], this.whitelist, this.blacklist, this.search)
            this.tree = tree
            this.searchResult = searchResult
            this.refresh()
        }
    }

}

/** 
 * Filter a tree according to white-listed prefixes, separated by commas
 */
function filterAndSearchTree(tree, whitelist, blacklist, search) {
    const _whitelist = StringUtils.commasToArray(whitelist)
    const _blacklist = StringUtils.commasToArray(blacklist)
    const _search = StringUtils.commasToArray(search)

    const searchCounts = {}
    _.forEach(_search, p => { searchCounts[p] = 0 })

    function sumValues(nodes) {
        const v = _.reduce(nodes, (sum, c) => sum + c.value, 0)
        return v
    }

    function searchNode(node, stopFlags) {
        const nextStopFlags = _.assign({}, stopFlags)
        var isMatch = false
        _.forEach(_search, p => {
            if (node.name.startsWith(p)) {
                if (!stopFlags[p]) {
                    // if prefix was found in a parent node, do not add its value to the global 
                    // count to not count the parent AND the children
                    searchCounts[p] += node.value
                    nextStopFlags[p] = true
                }
                isMatch = true
            }
        })
        return {
            isMatch: isMatch,
            nextStopFlags: nextStopFlags
        }
    }

    function recWhitelist(node) {
        if (_whitelist.length == 0 || _.some(_whitelist, p => node.name.startsWith(p))) {
            return recBlacklistAndSearch(node, {}) // node matches, do no go further, blacklist if necessary
        }
        if (node.children.length == 0) {
            return [] // reached a leaf, do not continue
        }
        return _.flatMap(node.children, c => recWhitelist(c))
    }

    function recBlacklistAndSearch(node, stopFlags) {
        if (_.some(_blacklist, p => node.name.startsWith(p))) {
            return [] // blacklisted, do not return
        }

        const { isMatch, nextStopFlags } = searchNode(node, stopFlags)
        const children = _.flatMap(node.children, c => recBlacklistAndSearch(c, nextStopFlags))
        const valueDiff = sumValues(node.children) - sumValues(children)

        return [_.assign({}, node, {
            children: children,
            value: node.value - valueDiff,
            highlight: isMatch
        })]
    }

    const children = _.flatMap(tree.children, c => recWhitelist(c))
    const valueDiff = sumValues(tree.children) - sumValues(children)
    const root = _.assign({}, tree, {
        children: children,
        value: tree.value - valueDiff,
        highlight: false
    })

    const searchResult = _.chain(searchCounts)
        .map((v, k) => ({ search: k, samples: v, precent: (v / root.value) * 100 }))
        .filter(o => o.samples > 0)
        .value()

    return {
        tree: root,
        searchResult: searchResult
    }
}

/**
 * Generate color for node
 */
function colorForNode(node) {
    const splits = _.chain(node.name.trim().split('\.'))
        .flatMap(n => n.split(':'))
        .value()
    // Return an rgb() color string that is a hash of the provided name,
    // and with a warm palette.
    var vector = generateHash(node.name)    // vector has 3 components between 0 and 1
    var r = 75 + Math.round(180 * vector[0])
    var g = 75 + Math.round(180 * vector[1])
    var b = 75 + Math.round(180 * vector[2])
    return "rgb(" + r + "," + g + "," + b + ")"
}

function generateHash(splits) {
    // Return a vector (0.0->1.0) that is a hash of the input splits.
    // The hash is computed to favor early splits over later ones
    var hashes = [0, 0, 0], max_hashes = [0, 0, 0], weight = 1
    const mods = [10, 11, 12], max_splits = 6

    for (var i = 0; i < splits.length; i++) {
        if (i > max_splits) { break }
        const hash = StringUtils.hashCode(splits[i])
        for (var j = 0; j < 3; j++) {
            hashes[j] = hashes[j] + weight * (hash % mods[j])
            max_hashes[j] = max_hashes[j] + weight * (mods[j] - 1)
        }
        weight *= 0.7
    }
    for (var j = 0; j < 3; j++) {
        hashes[j] = hashes[j] / max_hashes[j]
    }
    return hashes;
}
</script>

<style lang="scss">
.d3-flame-graph-label,
.d3-flame-graph-tip {
    font-family: 'Avenir', Helvetica, Arial, sans-serif;
}
</style>
