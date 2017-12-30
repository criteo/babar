package com.bhnte.babar.agent.profiler;

import com.bhnte.babar.agent.config.AgentConfig;
import com.bhnte.babar.agent.profiler.SamplingProfiler;
import com.bhnte.babar.agent.profiler.utils.MemoryUtils;
import com.bhnte.babar.agent.reporter.Reporter;

import java.lang.management.MemoryUsage;

/**
 * Profiles the memory used by the thread tree
 */
public class MemoryProfiler extends SamplingProfiler {

    protected final long reservedMB;

    public MemoryProfiler(AgentConfig agentConfig, Reporter reporter) {
        super(agentConfig, reporter);
        reservedMB = profilerConfig.getLongOrDefault("reservedMB", 0L);
    }

    @Override
    public void start(long startTimeMs) {
    }

    @Override
    public void stop(long stopTimeMs, long deltaLastSampleMs) throws Exception {
    }

    @Override
    public void sample(long sampleTimeMs, long deltaLastSampleMs) throws Exception {
        MemoryUsage heapUsage = MemoryUtils.getHeapMemoryUsed();
        MemoryUsage nonHeapUsage = MemoryUtils.getNonHeapMemoryUsed();
        reporter.reportEvent("HEAP_MEMORY_USED_BYTES", "", (double)heapUsage.getUsed(), sampleTimeMs);
        reporter.reportEvent("HEAP_MEMORY_COMMITTED_BYTES", "", (double)heapUsage.getCommitted(), sampleTimeMs);
        reporter.reportEvent("OFF_HEAP_MEMORY_USED_BYTES", "", (double)nonHeapUsage.getUsed(), sampleTimeMs);
        reporter.reportEvent("OFF_HEAP_MEMORY_COMMITTED_BYTES", "", (double)nonHeapUsage.getCommitted(), sampleTimeMs);
        reporter.reportEvent("MEMORY_RESERVED_BYTES", "", (double)reservedMB * 1024 * 1024, sampleTimeMs);
        reporter.reportEvent("HEAP_MEMORY_MAX_BYTES", "", (double)heapUsage.getMax(), sampleTimeMs);
    }
}
