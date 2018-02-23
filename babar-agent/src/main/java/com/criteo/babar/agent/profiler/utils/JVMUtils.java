package com.criteo.babar.agent.profiler.utils;

import java.lang.management.ManagementFactory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.management.OperatingSystemMXBean;

import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

public class JVMUtils {

    public static String MINOR_GC_ACTION = "end of minor GC";
    public static String MAJOR_GC_ACTION = "end of major GC";

    private static final OperatingSystemMXBean operatingSystemMXBean = (com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
    private static final RuntimeMXBean runtimeMXBean = java.lang.management.ManagementFactory.getRuntimeMXBean();

    private JVMUtils() {
    }

    /**
     * Get the accumulated JVM CPU time since start.
     * @return          The accumulated JVM CPU time since starting the JVM
     */
    public static long getAccumulatedJVMCPUTime() {
        return operatingSystemMXBean.getProcessCpuTime();
    }

    /**
     * Get the system CPU load
     * @return          The system cpu load
     */
    public static double getSystemCPULoad() {
        return operatingSystemMXBean.getSystemCpuLoad();
    }

    /**
     * get the number of available logical CPUs (or hyper threads)
     * @return          The number of CPUs
     */
    public static int getAvailableProcessors() {
        return operatingSystemMXBean.getAvailableProcessors();
    }

    /**
     * Returns the accumulated GC time in milliseconds for all available beans
     * @return          the accumulated GC time
     */
    public static long getAccumulatedGCTime() {
        long gcTime = 0L;
        for (GarbageCollectorMXBean bean: ManagementFactory.getGarbageCollectorMXBeans()) {
            gcTime += bean.getCollectionTime();
        }
        return gcTime;
    }

    /**
     * Register to all notifications from the GC MXBeans.
     * @param listener          A java NotificationListener
     */
    public static void registerGCListener(NotificationListener listener) {
        for (GarbageCollectorMXBean bean: ManagementFactory.getGarbageCollectorMXBeans()) {
            ((NotificationEmitter)bean).addNotificationListener(listener, null, null);
        }
    }
}
