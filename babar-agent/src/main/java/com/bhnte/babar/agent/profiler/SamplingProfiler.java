package com.bhnte.babar.agent.profiler;

import com.bhnte.babar.agent.config.AgentConfig;
import com.bhnte.babar.agent.reporter.Reporter;
import com.bhnte.babar.agent.worker.SamplingSchedulable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public abstract class SamplingProfiler extends Profiler implements SamplingSchedulable {

    protected final long profilingIntervalMs;
    protected final AtomicLong lastSampleTimeMs = new AtomicLong(0l);

    public SamplingProfiler(AgentConfig agentConfig, Reporter reporter) {
        super(agentConfig, reporter);
        this.profilingIntervalMs = Math.max(1, profilerConfig.getIntOrDefault("profilingMs", 1000));
    }

    @Override
    public void start() throws Exception {
        long currentTimeMs = System.currentTimeMillis();
        this.lastSampleTimeMs.set(currentTimeMs);
        try {
            this.start(currentTimeMs);
            LOG.debug("started");
        }
        catch (Exception e ) {
            LOG.error("Exception thrown while starting", e);
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
            LOG.debug("stopped");
        }
        catch (Exception e) {
            LOG.error("Exception thrown while stopping", e);
        }
    }

    public abstract void stop(long stopTimeMs, long deltaLastSampleMs) throws Exception;

    public void run() {
        long currentTimeMs = System.currentTimeMillis();
        long deltaSampleMs = currentTimeMs - this.lastSampleTimeMs.getAndSet(currentTimeMs);
        try {
            sampleInternal(currentTimeMs, deltaSampleMs);
        } catch (Exception e) {
            LOG.error("Exception thrown while running", e);
        }
    }

    public abstract void sample(long sampleTimeMs, long deltaLastSampleMs) throws Exception;

    protected void sampleInternal(long sampleTimeMs, long deltaLastSampleMs) throws Exception {
        LOG.debug("sampling");
        try {
            sample(sampleTimeMs, deltaLastSampleMs);
        }
        catch (Exception e) {
            LOG.error("Exception throws while sampling", e);
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
