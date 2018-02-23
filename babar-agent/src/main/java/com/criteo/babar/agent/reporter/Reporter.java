package com.criteo.babar.agent.reporter;

import com.criteo.babar.agent.config.AgentConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class Reporter {

    public abstract void start();
    public abstract void stop();
    public abstract void reportEvent(String metric, String label, Double value, Long time);

    protected final String container;

    public Reporter(AgentConfig config) {
        this.container = getContainerId();
        System.out.println("Using container id '" + container + "' in profiles");
    }

    protected String getContainerId() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            return hostname + "_" + String.valueOf(Math.round(Math.random() * 10000));
        } catch (UnknownHostException e) {
            System.err.println("Unable to get the hostname, using random value instead");
            e.printStackTrace();
            return String.valueOf(Math.round(Math.random() * 1000000000));
        }
    }
}
