package com.criteo.babar.agent.profiler;

import com.criteo.babar.agent.config.AgentConfig;
import com.criteo.babar.agent.config.Config;
import com.criteo.babar.agent.reporter.Reporter;
import com.criteo.babar.agent.worker.Schedulable;

public abstract class Profiler implements Schedulable {

    protected final Reporter reporter;
    protected final Config profilerConfig;

    public Profiler(AgentConfig agentConfig, Reporter reporter) {
        this.profilerConfig = agentConfig.getProfilerConfig(getClass().getSimpleName());
        this.reporter = reporter;
    }
}
