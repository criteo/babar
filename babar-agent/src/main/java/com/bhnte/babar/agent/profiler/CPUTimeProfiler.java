package com.bhnte.babar.agent.profiler;

import com.bhnte.babar.agent.config.AgentConfig;
import com.bhnte.babar.agent.profiler.utils.JVMUtils;
import com.bhnte.babar.agent.profiler.utils.ThreadUtils;
import com.bhnte.babar.agent.reporter.Reporter;
import com.sun.management.GarbageCollectionNotificationInfo;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.ThreadInfo;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import static com.bhnte.babar.agent.profiler.utils.JVMUtils.MAJOR_GC_ACTION;
import static com.bhnte.babar.agent.profiler.utils.JVMUtils.MINOR_GC_ACTION;

/**
 * Profiles the CPU used by the job
 */
public class CPUTimeProfiler extends SamplingProfiler {

    private static final long NANOS_TO_MILLIS = 1000000L;

    private final AtomicLong prevCpuTime = new AtomicLong(0L);
    private final AtomicLong prevGCTime = new AtomicLong(0L);
    private final AtomicLong minorGCTime = new AtomicLong(0L);
    private final AtomicLong majorGCTime = new AtomicLong(0L);


    private final int availableCpu = JVMUtils.getAvailableProcessors();

    public CPUTimeProfiler(AgentConfig agentConfig, Reporter reporter) {
        super(agentConfig, reporter);
    }

    @Override
    public void start(long startTimeMs) {
        // create a listener for GC notifications
        NotificationListener gcListener = new NotificationListener() {
            @Override
            public void handleNotification(Notification notification, Object handback) {
                if (!notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) return;

                // get the GC info object
                GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from((CompositeData)notification.getUserData());
                String gcAction = info.getGcAction();
                if (MINOR_GC_ACTION.equals(gcAction)) {
                    minorGCTime.addAndGet(info.getGcInfo().getDuration());
                }
                else if (MAJOR_GC_ACTION.equals(gcAction)) {
                    majorGCTime.addAndGet(info.getGcInfo().getDuration());
                }
            }
        };
        // register listener
        JVMUtils.registerGCListener(gcListener);
    }

    @Override
    public void stop(long stopTimeMs, long deltaLastSampleMs) throws Exception {
    }

    @Override
    public void sample(long sampleTimeMs, long deltaLastSampleMs) throws Exception {

        double deltaThreadsCpuTime = (double)updateThreadsCpuTimeAndGetDelta();
        // compute CPU usage in number of cores
        double nonCappedCpuUsage = deltaThreadsCpuTime / deltaLastSampleMs;
        double cpuUsage = Math.max(0D, nonCappedCpuUsage);

        double systemCpuLoad = JVMUtils.getSystemCPULoad();

        double deltaGcTime = (double)updateGCTimeAndGetDelta();

        double sampleMinorGCTime = minorGCTime.getAndSet(0L);
        double sampleMajorGCTime = majorGCTime.getAndSet(0L);
        double gcRatio = deltaGcTime / deltaThreadsCpuTime;
        double minorGCRatio = sampleMinorGCTime / deltaThreadsCpuTime;
        double majorGCRatio = sampleMajorGCTime / deltaThreadsCpuTime;

        reporter.reportEvent("GC_RATIO", "", gcRatio, sampleTimeMs);
        reporter.reportEvent("GC_SCALED_CPU_USAGE", "", gcRatio * cpuUsage, sampleTimeMs);
        reporter.reportEvent("MINOR_GC_RATIO", "", minorGCRatio, sampleTimeMs);
        reporter.reportEvent("MAJOR_GC_RATIO", "", majorGCRatio, sampleTimeMs);
        reporter.reportEvent("JVM_CPU_USAGE", "", cpuUsage / availableCpu, sampleTimeMs);
        reporter.reportEvent("JVM_SCALED_CPU_USAGE", "", cpuUsage, sampleTimeMs);
        reporter.reportEvent("SYSTEM_CPU_LOAD", "", systemCpuLoad, sampleTimeMs);
        reporter.reportEvent("SYSTEM_SCALED_CPU_USAGE", "", systemCpuLoad * availableCpu, sampleTimeMs);
    }

    private long updateThreadsCpuTimeAndGetDelta() {
        long totalThreadsCpuTime = JVMUtils.getAccumulatedJVMCPUTime() / NANOS_TO_MILLIS;
        // avoid first value being unrealistically large, set it to zero
        prevCpuTime.compareAndSet(0L, totalThreadsCpuTime);
        return Math.max(0, totalThreadsCpuTime - prevCpuTime.getAndSet(totalThreadsCpuTime));
    }

    private long updateGCTimeAndGetDelta() {
        Long totalGcTime = JVMUtils.getAccumulatedGCTime();
        // make sure first value is zero
        prevGCTime.compareAndSet(0L, totalGcTime);
        return Math.max(0, totalGcTime - prevGCTime.getAndSet(totalGcTime));
    }

    private Collection<ThreadInfo> getAllThreads() {
        return ThreadUtils.filterAllThreadsInState(false, false, Thread.State.RUNNABLE, ThreadUtils.EXCLUDED_THREADS_PREFIXES);
    }
}
