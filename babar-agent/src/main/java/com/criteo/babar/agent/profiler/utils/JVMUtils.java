package com.criteo.babar.agent.profiler.utils;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.OperatingSystemMXBean;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.*;

public class JVMUtils {

    private static final String MINOR_GC_ACTION = "end of minor GC";
    private static final String MAJOR_GC_ACTION = "end of major GC";

    private static final OperatingSystemMXBean operatingSystemMXBean = (com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
    private static final RuntimeMXBean runtimeMXBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

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
     * Register to all notifications from the GC MXBeans.
     * @param listener          A java NotificationListener
     */
    public static void registerGCListener(GCListener listener) {

        // takes the GC listener and makes javax NotificationListener that filters GC notifications
        NotificationListener notificationListener = new NotificationListener() {
            @Override
            public void handleNotification(Notification notification, Object handback) {
                if (!notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) return;

                // get the GC info object
                GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from((CompositeData)notification.getUserData());
                String gcAction = info.getGcAction();
                if (MINOR_GC_ACTION.equals(gcAction)) {
                    listener.onMinorGc(info.getGcInfo().getDuration());
                }
                else if (MAJOR_GC_ACTION.equals(gcAction)) {
                    listener.onMajorGc(info.getGcInfo().getDuration());
                }
            }
        };

        // register to the GC beans
        for (GarbageCollectorMXBean bean: ManagementFactory.getGarbageCollectorMXBeans()) {
            ((NotificationEmitter)bean).addNotificationListener(notificationListener, null, null);
        }
    }

    public interface GCListener {
        void onMinorGc(long duration);
        void onMajorGc(long duration);
    }

    /**
     * Returns the pid of the current JVM process
     * @return              The process pid of this JVM
     */
    public static int getPID() {
        String processName = runtimeMXBean.getName();
        try {
            return Integer.parseInt(processName.split("@")[0]);
        }
        catch (Exception e) {
            throw new RuntimeException("Can not parse pid from process name '" + processName + "'");
        }
    }

    /**
     * Get heap memory statistics
     */
    public static MemoryUsage getHeapMemoryUsed() {
        return memoryMXBean.getHeapMemoryUsage();
    }

    /**
     * Get off-heap memory statistics
     * @return
     */
    public static MemoryUsage getNonHeapMemoryUsed() {
        return memoryMXBean.getNonHeapMemoryUsage();
    }
}
