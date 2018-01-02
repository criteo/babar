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

    private final JsonFormat.Printer formatter = JsonFormat.printer();
    private final String LINE_PREFIX = "BABAR_METRIC\t";

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
        Gauge gauge = Gauge.newBuilder()
            .setContainer(container)
            .setMetric(metric)
            .setLabel(label)
            .setTimestamp(time)
            .setValue(value)
            .build();
        log(gauge);
    }

    private void log(MessageOrBuilder metric) {
        try {
            String line = formatter.print(metric).replace("\n", "");
            bw.write(LINE_PREFIX + line + "\n");
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
