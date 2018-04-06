package com.criteo.babar.agent.profiler;

import com.criteo.babar.agent.config.AgentConfig;
import com.criteo.babar.agent.profiler.utils.ThreadUtils;
import com.criteo.babar.agent.reporter.Reporter;
import com.criteo.babar.agent.worker.SamplingScheduler;
import com.google.common.base.Joiner;

import java.lang.management.ThreadInfo;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class StackTraceProfiler extends SamplingAggregatingProfiler {

    private final TraceCache traceCache;

    public StackTraceProfiler(AgentConfig agentConfig, Reporter reporter) {
        super(agentConfig, reporter);
        traceCache = new TraceCache();
    }

    @Override
    public void sample(long sampleTimeMs, long deltaLastSampleMs) {
        for (ThreadInfo thread : getAllRunnableThreads()) {
            // certain threads do not have stack traceCache
            if (thread.getStackTrace().length > 0) {
                String traceKey = formatStackTrace(thread.getThreadName(), thread.getStackTrace());
                traceCache.increment(traceKey);
            }
        }
    }

    @Override
    public void report() throws Exception {
        Map<String, Long> traces = traceCache.copyAndclear();
        for (Map.Entry<String, Long> trace: traces.entrySet()) {
            reporter.reportEvent("CPU_TRACES", trace.getKey(), trace.getValue().doubleValue(), System.currentTimeMillis());
        }
    }

    private Collection<ThreadInfo> getAllRunnableThreads() {
        return ThreadUtils.getAllThreads(false, false,
                t -> t.getThreadState() == Thread.State.RUNNABLE &&
                        !t.getThreadName().startsWith(SamplingScheduler.SCHEDULER_THREAD_PREFIX)
        );
    }

    @Override
    public void start(long startTimeMs) {
    }

    @Override
    public void stop(long stopTimeMs, long deltaLastSampleMs) throws Exception {
        report();
    }

    private static String formatStackTrace(String threadName, StackTraceElement[] stack) {
        Deque<String> lines = new ArrayDeque<>(stack.length);
        for (StackTraceElement element : stack) {
            lines.addFirst(formatStackTraceElement(element));
        }
        lines.addFirst(threadName);

        return Joiner.on("|").join(lines);
    }

    private static String formatStackTraceElement(StackTraceElement element) {
        return String.format("%s.%s:%d", element.getClassName(), element.getMethodName(), element.getLineNumber());
    }

    private class TraceCache {
        // instead of using ConcurrentHashMap, we synchronize on the map object before
        // each operation since ConcurrentHashMap does not provide an atomic equivalent to
        // copyAndclear()
        private Map<String, Long> traces = new HashMap<>();

        void increment(String trace) {
            synchronized (traces) {
                traces.compute(trace, (k, v) -> ((v != null) ? v : 0L) + 1L);
            }
        }

        Map<String, Long> copyAndclear() {
            Map<String, Long> tmp;
            synchronized (traces) {
                tmp = traces;
                traces = new HashMap<>(tmp.size());
            }
            return tmp;
        }
    }
}
