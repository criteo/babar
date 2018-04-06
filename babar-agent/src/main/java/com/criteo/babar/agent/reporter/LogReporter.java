package com.criteo.babar.agent.reporter;

import com.criteo.babar.agent.config.AgentConfig;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class LogReporter extends Reporter {

    public static final String LOG_DIR_KEY = "dir";

    private final static String LINE_PREFIX = "BABAR";
    private final static String LINE_SEPARATOR = "\t";

    private final String logDir;
    private BufferedWriter bw = null;
    private FileWriter fw = null;

    public LogReporter(AgentConfig config) {
        super(config);
        this.logDir = getLogDir(config);
    }

    @Override
    public void start() {
        try {
            fw = new FileWriter(logDir +"/babar.log");
            bw = new BufferedWriter(fw);
            bw.newLine();
        }
        catch (IOException e) {
            System.err.println("Exception thrown while tarting reporter");
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            bw.flush();
            bw.close();
            fw.close();
        }
        catch (IOException e) {
            System.err.println("Exception thrown while stopping reporter");
            e.printStackTrace();
        }
    }

    @Override
    public void reportEvent(String metric, String label, Double value, Long time) {
        String line = container + LINE_SEPARATOR + metric + LINE_SEPARATOR + time + LINE_SEPARATOR + value + LINE_SEPARATOR + label;
        log(line);
    }

    private void log(String line) {
        try {
            bw.write(LINE_PREFIX + LINE_SEPARATOR + line + "\n");
        }
        catch (Exception e) {
            System.err.println("Exception thrown while logging metric");
            e.printStackTrace();
        }
    }

    static private String getLogDir(AgentConfig config) {
        String logDir = config.getString(LOG_DIR_KEY);
        if (logDir == null) logDir = System.getProperty("yarn.app.container.log.dir");
        if (logDir == null) logDir = System.getProperty("spark.yarn.app.container.log.dir");
        if (logDir == null) logDir = "./log";
        return logDir;
    }
}
