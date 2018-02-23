package com.criteo.babar.agent.profiler.utils;

import com.criteo.babar.agent.worker.SamplingScheduler;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.Collection;

public class ThreadUtils {

    private static com.sun.management.ThreadMXBean threadMxBean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();

    public static final String[] EXCLUDED_THREADS_PREFIXES = new String[]{SamplingScheduler.SCHEDULER_THREAD_PREFIX};

    private ThreadUtils() {
    }

    private static class ThreadStatePredicate implements Predicate<ThreadInfo> {
        private final Thread.State state;

        ThreadStatePredicate(Thread.State state) {
            this.state = state;
        }

        @Override
        public boolean apply(ThreadInfo input) {
            return input.getThreadState() == state;
        }
    }

    /**
     * Dump state of all threads
     *
     * @param lockedMonitors      If true, dump all locked monitors
     * @param lockedSynchronizers If true, dump all locked ownable synchronizers
     * @return A Collection of {@link ThreadInfo} for all live threads
     */
    public static Collection<ThreadInfo> getAllThreads(boolean lockedMonitors, boolean lockedSynchronizers) {
        return Arrays.asList(threadMxBean.dumpAllThreads(lockedMonitors, lockedSynchronizers));
    }

    /**
     * Dump state of all threads with a given state
     *
     * @param lockedMonitors      If true, dump all locked monitors
     * @param lockedSynchronizers If true, dump all locked ownable synchronizers
     * @param state               The state in which a thread must be to be dumped
     * @return A Collection of {@link ThreadInfo} for all live threads in the given state
     */
    public static Collection<ThreadInfo> getAllThreadsInState(boolean lockedMonitors, boolean lockedSynchronizers, Thread.State state) {
        return Collections2.filter(getAllThreads(lockedMonitors, lockedSynchronizers), new ThreadStatePredicate(state));
    }

    /**
     * Dump state of all threads with a given state that match a predicate
     *
     * @param lockedMonitors      If true, dump all locked monitors
     * @param lockedSynchronizers If true, dump all locked ownable synchronizers
     * @param state               The state in which a thread must be to be dumped
     * @param excludedThreadsPrefixes   Prefixes of threads to exclude
     * @return A Collection of {@link ThreadInfo} for all live threads in the given state that match the given predicate
     */
    public static Collection<ThreadInfo> filterAllThreadsInState(boolean lockedMonitors, boolean lockedSynchronizers, Thread.State state, String[] excludedThreadsPrefixes) {
        return Collections2.filter(getAllThreadsInState(lockedMonitors, lockedSynchronizers, state), new Predicate<ThreadInfo>() {
            @Override
            public boolean apply(ThreadInfo input) {
                // do not sample babar threads
                for (String excludedPrefix: excludedThreadsPrefixes) {
                    if (input.getThreadName().startsWith(excludedPrefix)) return false;
                }
                return true;
            }
        });
    }
}
