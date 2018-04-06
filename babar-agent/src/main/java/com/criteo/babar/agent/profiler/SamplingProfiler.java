package com.criteo.babar.agent.profiler;

import com.criteo.babar.agent.config.AgentConfig;
import com.criteo.babar.agent.reporter.Reporter;
import com.criteo.babar.agent.worker.SamplingSchedulable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public abstract class SamplingProfiler extends Profiler implements SamplingSchedulable {

    protected final long profilingIntervalMs;
    protected final AtomicLong lastSampleTimeMs = new AtomicLong(0l);

    public SamplingProfiler(AgentConfig agentConfig, Reporter reporter) {
        super(agentConfig, reporter);
        this.profilingIntervalMs = Math.max(1, profilerConfig.getIntOrDefault("profilingMs", 1000));
        System.out.println(getClass().getName() +  " will sample every " + profilingIntervalMs + " ms");
    }

    @Override
    public void start() throws Exception {
        long currentTimeMs = System.currentTimeMillis();
        this.lastSampleTimeMs.set(currentTimeMs);
        try {
            this.start(currentTimeMs);
        }
        catch (Exception e ) {
            System.err.println("Exception thrown while starting profiler");
            e.printStackTrace();
        }
    }

    public abstract void start(long startTimeMs) throws Exception;

    @Override
    public void stop() throws Exception {
        long currentTimeMs = System.currentTimeMillis();
        long deltaSampleMs = currentTimeMs - this.lastSampleTimeMs.getAndSet(currentTimeMs);
        this.sampleInternal(currentTimeMs, deltaSampleMs);
        try {
            this.stop(currentTimeMs, deltaSampleMs);
        }
        catch (Exception e) {
            System.err.println("Exception thrown while stopping profiler");
            e.printStackTrace();
        }
    }

    public abstract void stop(long stopTimeMs, long deltaLastSampleMs) throws Exception;

    public void run() {
        long currentTimeMs = System.currentTimeMillis();
        long deltaSampleMs = currentTimeMs - this.lastSampleTimeMs.getAndSet(currentTimeMs);
        try {
            sampleInternal(currentTimeMs, deltaSampleMs);
        } catch (Exception e) {
            System.err.println("Exception thrown while profiling");
            e.printStackTrace();
        }
    }

    public abstract void sample(long sampleTimeMs, long deltaLastSampleMs) throws Exception;

    protected void sampleInternal(long sampleTimeMs, long deltaLastSampleMs) throws Exception {
        try {
            sample(sampleTimeMs, deltaLastSampleMs);
        }
        catch (Exception e) {
            System.err.println("Exception thrown while sampling");
            e.printStackTrace();
        }
    }

    protected long computeDelta(long currentTimeMs) {
        return currentTimeMs - this.lastSampleTimeMs.getAndSet(currentTimeMs);
    }

    public long getInterval() {
        return profilingIntervalMs;
    }

    public TimeUnit getTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }
}
