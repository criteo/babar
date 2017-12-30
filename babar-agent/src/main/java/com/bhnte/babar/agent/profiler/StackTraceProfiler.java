package com.bhnte.babar.agent.profiler;

import com.bhnte.babar.agent.config.AgentConfig;
import com.bhnte.babar.agent.profiler.utils.ThreadUtils;
import com.bhnte.babar.agent.reporter.Reporter;

import java.lang.management.ThreadInfo;
import java.util.*;

public class StackTraceProfiler extends SamplingAggregatingProfiler {

    private static final List<String> EXCLUDE_PACKAGES = Arrays.asList("com.bhnte", "io.grpc");

    private final StackTraceFilter filter;
    private TraceCache traceCache;

    public StackTraceProfiler(AgentConfig agentConfig, Reporter reporter) {
        super(agentConfig, reporter);

        filter = new StackTraceFilter(new ArrayList<String>(), EXCLUDE_PACKAGES);
        traceCache = new TraceCache();
    }

    @Override
    public void sample(long sampleTimeMs, long deltaLastSampleMs) {
        for (ThreadInfo thread : getAllRunnableThreads()) {
            // certain threads do not have stack traceCache
            if (thread.getStackTrace().length > 0) {
                String traceKey = StackTraceFormatter.formatStackTrace(thread.getThreadName(), thread.getStackTrace());
                if (filter.includeStackTrace(traceKey)) {
                    traceCache.increment(traceKey);
                }
            }
        }
    }

    @Override
    public void report() throws Exception {
        Map<String, Long> traces = traceCache.clear();
        for (Map.Entry<String, Long> trace: traces.entrySet()) {
            reporter.reportEvent("CPU_TRACES", trace.getKey(), trace.getValue().doubleValue(), System.currentTimeMillis());
        }
    }

    private static Collection<ThreadInfo> getAllRunnableThreads() {
        return ThreadUtils.filterAllThreadsInState(false, false, Thread.State.RUNNABLE, ThreadUtils.EXCLUDED_THREADS_PREFIXES);
    }

    @Override
    public void start(long startTimeMs) {
    }

    @Override
    public void stop(long stopTimeMs, long deltaLastSampleMs) throws Exception {
        report();
    }

    private class TraceCache {
        private Map<String, Long> traces = new HashMap<>();

        void increment(String trace) {
            synchronized (traces) {
                traces.put(trace, traces.getOrDefault(trace, 0l) + 1l);
            }
        }

        Map<String, Long> clear() {
            Map<String, Long> tmp;
            synchronized (traces) {
                tmp = traces;
                traces = new HashMap<>(tmp.size());
            }
            return tmp;
        }
    }
}
