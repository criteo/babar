package com.criteo.babar.agent;

import com.criteo.babar.agent.config.AgentConfig;
import com.criteo.babar.agent.profiler.CPUTimeProfiler;
import com.criteo.babar.agent.profiler.MemoryProfiler;
import com.criteo.babar.agent.profiler.Profiler;
import com.criteo.babar.agent.profiler.SamplingProfiler;
import com.criteo.babar.agent.profiler.StackTraceProfiler;
import com.criteo.babar.agent.reporter.LogReporter;
import com.criteo.babar.agent.reporter.Reporter;
import com.criteo.babar.agent.worker.SamplingScheduler;
import com.criteo.babar.agent.worker.ShutdownHookWorker;

import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Set;

public class Agent {

    private Agent() { }

    private static Set<SamplingProfiler> samplingProfilers = new HashSet<>();
    private static Set<Profiler> profilers = new HashSet<>();

    public static void agentmain(final String args, final Instrumentation instrumentation) {
        premain(args, instrumentation);
    }

    /**
     * Start the JVM profiler
     */
    public static void premain(final String args, final Instrumentation instrumentation) {

        // parse agent arguments
        AgentConfig config = AgentConfig.parse(args);
        Reporter reporter = new LogReporter(config);

        // register profilers for scheduling
        if (config.isProfilerEnabled(StackTraceProfiler.class.getSimpleName())) {
            registerProfiler(new StackTraceProfiler(config, reporter));
        }
        if (config.isProfilerEnabled(MemoryProfiler.class.getSimpleName())) {
            registerProfiler(new MemoryProfiler(config, reporter));
        }
        if (config.isProfilerEnabled(CPUTimeProfiler.class.getSimpleName())) {
            registerProfiler(new CPUTimeProfiler(config, reporter));
        }

        // open reporter if required
        reporter.start();

        // start the profilers. They will be able to profile with their start() methods
        startStartStopProfilers(reporter);
        startSamplingProfilers();
    }

    private static void registerProfiler(Profiler p) {
        profilers.add(p);
        if (p instanceof SamplingProfiler) samplingProfilers.add((SamplingProfiler)p);
    }

    private static void startStartStopProfilers(Reporter reporter) {
        for (Profiler p: profilers){
            try {
                p.start();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        Thread shutdownHook = new Thread(new ShutdownHookWorker(profilers, reporter));
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private static void startSamplingProfilers() {
        SamplingScheduler profilerSamplingScheduler = new SamplingScheduler();
        profilerSamplingScheduler.schedule(samplingProfilers);
    }
}
