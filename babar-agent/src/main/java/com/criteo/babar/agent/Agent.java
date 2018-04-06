package com.criteo.babar.agent;

import com.criteo.babar.agent.config.AgentConfig;
import com.criteo.babar.agent.profiler.JVMProfiler;
import com.criteo.babar.agent.profiler.ProcFSProfiler;
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

    public static void agentmain(final String args, final Instrumentation instrumentation) {
        premain(args, instrumentation);
    }

    /**
     * Start the JVM agent
     */
    public static void premain(final String args, final Instrumentation instrumentation) {

        // parse agent arguments
        AgentConfig config = AgentConfig.parse(args);

        // open reporter if required
        Reporter reporter = new LogReporter(config);
        reporter.start();

        // register profilers for scheduling
        Set<Profiler> profilers = new HashSet<>();

        if (config.isProfilerEnabled(ProcFSProfiler.class.getSimpleName())) {
            profilers.add(new ProcFSProfiler(config, reporter));
        }
        if (config.isProfilerEnabled(JVMProfiler.class.getSimpleName())) {
            profilers.add(new JVMProfiler(config, reporter));
        }
        if (config.isProfilerEnabled(StackTraceProfiler.class.getSimpleName())) {
            profilers.add(new StackTraceProfiler(config, reporter));
        }

        // start the profilers. They will be able to profile with their start() methods
        startProfilers(profilers);

        // add shutdown hook to correctly stop profilers and report last values on exit
        registerShutdownHook(profilers, reporter);
    }

    private static void startProfilers(Set<Profiler> profilers) {
        SamplingScheduler profilerSamplingScheduler = new SamplingScheduler();
        Set<SamplingProfiler> samplingProfilers = new HashSet<>();
        for (Profiler p: profilers){
            try {
                p.start();
                if (p instanceof SamplingProfiler) {
                    samplingProfilers.add((SamplingProfiler)p);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        // start sampling profilers
        profilerSamplingScheduler.schedule(samplingProfilers);
    }

    private static void registerShutdownHook(Set<Profiler> profilers, Reporter reporter) {
        Thread shutdownHook = new Thread(new ShutdownHookWorker(profilers, reporter));
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
}
