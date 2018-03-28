package com.criteo.babar.agent.profiler;

import com.criteo.babar.agent.config.AgentConfig;
import com.criteo.babar.agent.profiler.utils.JVMUtils;
import com.criteo.babar.agent.reporter.Reporter;

import java.lang.management.MemoryUsage;
import java.util.concurrent.atomic.AtomicLong;

public class JVMProfiler extends SamplingProfiler {

    public final String RESERVED_MB = "reservedMB";

    private static final long NANOS_TO_MILLIS = 1000000L;

    private final int availableCpu = JVMUtils.getAvailableProcessors();
    private final long reservedMB;

    private final AtomicLong prevJvmCpuTime = new AtomicLong(0L);
    private final AtomicLong prevGcTime = new AtomicLong(0L);
    private final AtomicLong minorGcTime = new AtomicLong(0L);
    private final AtomicLong majorGcTime = new AtomicLong(0L);

    public JVMProfiler(AgentConfig agentConfig, Reporter reporter) {
        super(agentConfig, reporter);
        reservedMB = profilerConfig.getLongOrDefault(RESERVED_MB, 0L);
    }

    @Override
    public void start(long startTimeMs) throws Exception {
        // register a GC listener to get updates on GC times
        JVMUtils.registerGCListener(new JVMUtils.GCListener() {
            @Override
            public void onMinorGc(long duration) {
                minorGcTime.addAndGet(duration);
            }

            @Override
            public void onMajorGc(long duration) {
                majorGcTime.addAndGet(duration);
            }
        });

        // CPU: Set first values
        long totalJvmCpuTime = JVMUtils.getAccumulatedJVMCPUTime() / NANOS_TO_MILLIS;
        prevJvmCpuTime.set(totalJvmCpuTime);
    }

    @Override
    public void sample(long sampleTimeMs, long deltaLastSampleMs) throws Exception {
        // Get all values first
        // CPU
        long totalJvmCpuTime = JVMUtils.getAccumulatedJVMCPUTime() / NANOS_TO_MILLIS;
        double hostCpuLoad = JVMUtils.getSystemCPULoad();
        // GC
        long totalGcTime = JVMUtils.getAccumulatedGCTime();
        double deltaMinorGcTime = minorGcTime.getAndSet(0L);
        double deltaMajorGcTime = majorGcTime.getAndSet(0L);
        // Memory
        MemoryUsage heapUsage = JVMUtils.getHeapMemoryUsed();
        MemoryUsage nonHeapUsage = JVMUtils.getNonHeapMemoryUsed();

        // Compute metrics once values are fetched
        // CPU
        long deltaJvmCpuTime = totalJvmCpuTime - prevJvmCpuTime.getAndSet(totalJvmCpuTime);
        double jvmCoresUsage = deltaJvmCpuTime / (double)deltaLastSampleMs;
        double jvmCpuUsage = jvmCoresUsage / availableCpu;
        double hostCoresLoad = hostCpuLoad * availableCpu;
        // GC
        long deltaGcTime = totalGcTime - prevGcTime.getAndSet(totalGcTime);
        double gcRatio = totalGcTime / (double)deltaLastSampleMs;
        double minorGcRatio = deltaMinorGcTime / (double)deltaLastSampleMs;
        double majorGcRatio = deltaMajorGcTime / (double)deltaLastSampleMs;
        // Memory
        double usedHeapBytes = heapUsage.getUsed();
        double committedHeapBytes = heapUsage.getCommitted();
        double usedOffHeapBytes = nonHeapUsage.getUsed();
        double committedOffHeapBytes = nonHeapUsage.getCommitted();
        double reservedBytes = reservedMB * 1024D * 1024D;

        // report computed metrics
        // CPU
        reporter.reportEvent("JVM_CPU_USAGE", "", jvmCpuUsage, sampleTimeMs);
        reporter.reportEvent("JVM_CORES_USAGE", "", jvmCoresUsage, sampleTimeMs);
        reporter.reportEvent("JVM_HOST_CPU_USAGE", "", hostCpuLoad, sampleTimeMs);
        reporter.reportEvent("JVM_HOST_CORES_USAGE", "", hostCoresLoad, sampleTimeMs);
        // GC
        reporter.reportEvent("JVM_GC_RATIO", "", gcRatio, sampleTimeMs);
        reporter.reportEvent("JVM_MINOR_GC_RATIO", "", minorGcRatio, sampleTimeMs);
        reporter.reportEvent("JVM_MAJOR_GC_RATIO", "", majorGcRatio, sampleTimeMs);
        // Memory
        reporter.reportEvent("JVM_HEAP_MEMORY_USED_BYTES", "", usedHeapBytes, sampleTimeMs);
        reporter.reportEvent("JVM_HEAP_MEMORY_COMMITTED_BYTES", "", committedHeapBytes, sampleTimeMs);
        reporter.reportEvent("JVM_OFF_HEAP_MEMORY_USED_BYTES", "", usedOffHeapBytes, sampleTimeMs);
        reporter.reportEvent("JVM_OFF_HEAP_MEMORY_COMMITTED_BYTES", "", committedOffHeapBytes, sampleTimeMs);
        reporter.reportEvent("JVM_MEMORY_RESERVED_BYTES", "", reservedBytes, sampleTimeMs);
     }

    @Override
    public void stop(long stopTimeMs, long deltaLastSampleMs) throws Exception {

    }

}
