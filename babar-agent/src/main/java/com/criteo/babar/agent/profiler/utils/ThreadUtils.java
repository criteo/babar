package com.criteo.babar.agent.profiler.utils;

import com.sun.management.ThreadMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ThreadUtils {

    private final static ThreadMXBean threadMxBean = (ThreadMXBean)ManagementFactory.getThreadMXBean();

    private ThreadUtils() {
    }

    /**
     * Dump state of all threads
     *
     * @param lockedMonitors        If true, dump all locked monitors
     * @param lockedSynchronizers   If true, dump all locked ownable synchronizers
     * @return                      A List of {@link ThreadInfo} for all live threads
     */
    public static List<ThreadInfo> getAllThreads(boolean lockedMonitors, boolean lockedSynchronizers) {
        return Arrays.asList(threadMxBean.dumpAllThreads(lockedMonitors, lockedSynchronizers));
    }

    /**
     * Dump state of all threads
     *
     * @param lockedMonitors        If true, dump all locked monitors
     * @param lockedSynchronizers   If true, dump all locked ownable synchronizers
     * @param filter                Apply filter to the threads
     * @return                      A List of {@link ThreadInfo} for all live threads
     */
    public static List<ThreadInfo> getAllThreads(boolean lockedMonitors,
                                                 boolean lockedSynchronizers,
                                                 final Predicate<ThreadInfo> filter) {
        return Arrays.stream(threadMxBean.dumpAllThreads(lockedMonitors, lockedSynchronizers))
                .filter(filter)
                .collect(Collectors.toList());
    }
}
