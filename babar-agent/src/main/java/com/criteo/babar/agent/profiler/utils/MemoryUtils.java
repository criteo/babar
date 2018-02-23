package com.criteo.babar.agent.profiler.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class MemoryUtils {

    private static MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    private MemoryUtils() {
    }

    public static MemoryUsage getHeapMemoryUsed() {
        return memoryMXBean.getHeapMemoryUsage();
    }

    public static MemoryUsage getNonHeapMemoryUsed() {
        return memoryMXBean.getNonHeapMemoryUsage();
    }
}
