package com.criteo.babar.agent.profiler.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class OSUtils {

    private static final OPERATING_SYSTEM OS;

    public enum OPERATING_SYSTEM {
        UNKNOWN, LINUX, WINDOWS, MACOS;
    }

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) OS = OPERATING_SYSTEM.WINDOWS;
        else if (osName.contains("mac")) OS = OPERATING_SYSTEM.MACOS;
        else if (osName.contains("linux")) OS = OPERATING_SYSTEM.LINUX;
        else OS = OPERATING_SYSTEM.UNKNOWN;
    }

    /**
     * Executes a command and returns the standard output
     * @param commands              The command to execute (split by spaces)
     * @return                      The standard output of the process
     */
    public static String exec(String[] commands) throws IOException {
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(commands);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        StringBuilder sb  = new StringBuilder();
        String line;
        while ((line = stdInput.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Get an OS configuration value using the native `getConf` command
     * @param key               The configuration key
     * @return                  The configuration value or null if the key is not found
     */
    public static String getConf(String key) throws IOException, IllegalAccessException {
        if (OS != OPERATING_SYSTEM.LINUX && OS != OPERATING_SYSTEM.MACOS) {
            // the getConf command does exist on mac and linux
            throw new IllegalAccessException("getconf is not compatbile with the os (" + OS.name() + ")");
        }
        String res = exec(new String[]{"getconf", key});
        if (res.startsWith("getconf: Unrecognized variable")) {
            // key not found
            throw new IllegalArgumentException("key '" + key + "' not found in os configuration");
        }
        return res;
    }

    public static OPERATING_SYSTEM getOS() {
        return OS;
    }

    /**
     * Returns the memory page size. Usually 4096 bytes by default.
     */
    public static long getPageSizeBytes() throws IOException, IllegalAccessException {
        return Long.parseLong( getConf("PAGESIZE").trim() );
    }

    /**
     * Returns the number of clock ticks per seconds for the processor
     */
    public static long getJiffiesPerSecond() throws IOException, IllegalAccessException {
        return Long.parseLong( getConf("CLK_TCK").trim() );
    }

    /**
     * return the interval between clock ticks in miliseconds
     */
    public static long getJiffyLengthInMillis() throws IOException, IllegalAccessException {
        return Math.round(1000D / getJiffiesPerSecond());
    }

}
