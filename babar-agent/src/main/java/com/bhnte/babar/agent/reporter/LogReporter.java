package com.bhnte.babar.agent.reporter;

import com.bhnte.babar.agent.config.AgentConfig;
import com.bhnte.babar.api.metrics.Gauge;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class LogReporter extends Reporter {

    private final Logger logger = Logger.getLogger(getClass());

    private final static String LINE_PREFIX = "BABAR";
    private final static String LINE_SEPARATOR = "\t";

    private static final String logDir = getLogDir();
    private BufferedWriter bw = null;
    private FileWriter fw = null;

    public LogReporter(AgentConfig config) {
        super(config);
    }

    @Override
    public void start() {
        try {
            fw = new FileWriter(logDir +"/babar.log");
            bw = new BufferedWriter(fw);
            bw.newLine();
        }
        catch (IOException e) {
            logger.error("Error starting log reporter", e);
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
            logger.error("Error stopping log reporter", e);
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
            logger.error("Error logging metric", e);
        }
    }

    static private String getLogDir() {
        String logDir = System.getProperty("yarn.app.container.log.dir");
        if (logDir == null) logDir = System.getProperty("spark.yarn.app.container.log.dir");
        if (logDir == null) logDir = "./log";
        return logDir;
    }
}
