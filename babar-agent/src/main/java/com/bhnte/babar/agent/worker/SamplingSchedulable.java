package com.bhnte.babar.agent.worker;

import java.util.concurrent.TimeUnit;

public interface SamplingSchedulable extends Runnable, Schedulable {

    long getInterval();

    TimeUnit getTimeUnit();
}
