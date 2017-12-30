package com.bhnte.babar.agent.worker;

public interface Schedulable {

    /**
     * Start will be synchronously called when the profiler is first scheduled,
     * before the Main class of the app is called.
     */
    void start() throws Exception;

    /**
     * Stop will be called asynchronously when the shutdown routine has been called
     */
    void stop()  throws Exception;
}
