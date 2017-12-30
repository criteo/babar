package com.bhnte.babar.agent.worker;

import com.google.common.util.concurrent.MoreExecutors;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class SamplingScheduler {

    public static String SCHEDULER_THREAD_PREFIX = "babar-sampling";

    public void schedule(Set<? extends SamplingSchedulable> schedulables) {

        ScheduledExecutorService scheduledExecutorService = MoreExecutors.getExitingScheduledExecutorService(
                (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(schedulables.size(), new SchedulerThreadFactory()), 0, TimeUnit.MILLISECONDS);

        for (SamplingSchedulable samplingSchedulable : schedulables) {
            System.out.println("scheduling "+ samplingSchedulable.getClass()+ " with an interval of "+ samplingSchedulable.getInterval() + " ms");
            try {
                scheduledExecutorService.scheduleAtFixedRate(samplingSchedulable, 0, samplingSchedulable.getInterval(), samplingSchedulable.getTimeUnit());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class SchedulerThreadFactory implements ThreadFactory {

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(SCHEDULER_THREAD_PREFIX + "-" + t.getName());
            return t;
        }
    }
}
