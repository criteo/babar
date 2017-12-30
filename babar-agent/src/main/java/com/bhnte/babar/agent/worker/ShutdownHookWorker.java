package com.bhnte.babar.agent.worker;

import com.bhnte.babar.agent.reporter.Reporter;
import org.apache.log4j.LogManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class ShutdownHookWorker implements Runnable {

    private final Collection<? extends Schedulable> schedulables;
    private final Reporter reporter;

    public ShutdownHookWorker(Collection<? extends Schedulable> schedulables, Reporter reporter) {
        this.schedulables = schedulables;
        this.reporter = reporter;
    }

    @Override
    public void run() {

        // profile for all profilers
        for (Schedulable s: schedulables) {
            try {
                s.stop();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        // finally stop reporter correctly
        reporter.stop();
    }
}
