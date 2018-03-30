<template>
    <div class="plot-time-series" :id="plotId">
    </div>
</template>

<script>
import Vue from 'vue'
import BootstrapVue from 'bootstrap-vue'
import $ from 'jquery'
import echarts from 'echarts'
import _ from 'lodash'

export default {
    props: {
        title: {
            required: true
        },
        series: {
            default: () => [],
        }
    },
    data() {
        return {
            plotId: "plot-" + this.title
        }
    },
    mounted() {
        this.myChart = echarts.init(document.getElementById(this.plotId));
        // draw chart
        this.myChart.setOption({
            title: {
                text: this.title,
                x: 'center'
            },
            tooltip: {
                trigger: 'axis'
            },
            legend: {
                data: this.series.map(s => s.name),
                top: '5%'
            },
            grid: {
                left: '100px',
                right: '80px',
                top: '60px',
                bottom: '50px',
                containLabel: false
            },
            xAxis: {
                type: 'time'
            },
            yAxis: {
                type: 'value',
                name: "YAXIS"
            },
            series: this.series.map(s => ({
                name: s.name,
                type: 'line',
                symbolSize: 4,
                hoverAnimation: false,
                stack: s.stack,
                areaStyle: { normal: { opacity: 1 } },
                data: _.zip(s.time, s.values).map(v => ({ value: v }))
            }))
        });
    },
    destroyed() {
        this.myChart && this.myChart.dispose()
    }
}
</script>

<style lang="scss">
.plot-time-series {
    height: 400px;
}
</style>
