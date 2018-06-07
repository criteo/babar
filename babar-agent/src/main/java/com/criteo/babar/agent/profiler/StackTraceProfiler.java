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
    protected int getDefaultProfilingMs() {
        return 20; // profile every 20 ms
    }

    @Override
    protected int getDefaultReportingMs() {
        return 60 * 1000 * 10; // report every 10 minutes as stack traces are heavy
    }

    @Override
    public void sample(long sampleTimeMs, long deltaLastSampleMs) {
        for (ThreadInfo thread : getAllRunnableThreads()) {
            // certain threads do not have stack traceCache
            if (thread.getStackTrace().length > 0) {
                String traceKey = formatStackTrace(thread.getThreadName(), thread.getStackTrace());
                traceCache.increment(traceKey, sampleTimeMs);
            }
        }
    }

    @Override
    public void report() throws Exception {
        Map<String, Trace> traces = traceCache.copyAndClear();
        for (Map.Entry<String, Trace> trace: traces.entrySet()) {
            reporter.reportEvent("CPU_TRACES", trace.getKey(), (double)trace.getValue().count, trace.getValue().firstTimestamp);
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
        // copyAndClear()
        private Map<String, Trace> traces = new HashMap<>();

        void increment(String trace, long timestamp) {
            synchronized (traces) {
                Trace cur = traces.get(trace);
                if (cur == null) {
                    // not in map
                    cur = new Trace();
                    traces.put(trace, cur);
                }
                cur.inc(1L, timestamp);
            }
        }

        Map<String, Trace> copyAndClear() {
            Map<String, Trace> tmp;
            synchronized (traces) {
                tmp = traces;
                traces = new HashMap<>(tmp.size());
            }
            return tmp;
        }
    }

    private class Trace {
        public long count = 0L;
        public long firstTimestamp = -1L;

        public void inc(long amount, long timestamp) {
            count += amount;
            if (firstTimestamp <= 0L || firstTimestamp > timestamp) {
                firstTimestamp = timestamp;
            }
        }
    }
}
