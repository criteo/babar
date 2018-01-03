package com.bhnte.babar.agent.profiler;

import com.bhnte.babar.agent.config.AgentConfig;
import com.bhnte.babar.agent.config.Config;
import com.bhnte.babar.agent.reporter.Reporter;
import com.bhnte.babar.agent.worker.Schedulable;

public abstract class Profiler implements Schedulable {

    protected final Reporter reporter;
    protected final Config profilerConfig;

    public Profiler(AgentConfig agentConfig, Reporter reporter) {
        this.profilerConfig = agentConfig.getProfilerConfig(getClass().getSimpleName());
        this.reporter = reporter;
    }
}
