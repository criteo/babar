package com.criteo.babar.agent.profiler.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcFSUtils {

    private static final Path PROC_DIR = Paths.get("/proc");

    public static boolean isAvailable() {
        // currently only available on linux as we parse the content of the /proc fs that is OS-dependent
        return OSUtils.getOS() == OSUtils.OPERATING_SYSTEM.LINUX &&
                Files.isDirectory(PROC_DIR) &&
                Files.isReadable(PROC_DIR);
    }

    /**
     * Get the pids of all the processes in the process tree of the given pid, including this pid.
     * @param rootPid               The root process of the tree
     * @return                      An array of pids
     */
    public static int[] getChildrenPids(int rootPid) throws IOException {
        String output = OSUtils.exec(new String[]{"ps", "-o pid --no-headers --ppid " + String.valueOf(rootPid)});
        return parsePidsOutput(rootPid, output);
    }

    protected static int[] parsePidsOutput(int rootPid, String output) {
        String[] pidsStrings = output.split("\n");
        if (output.isEmpty()) return new int[]{rootPid};

        int[] pids = new int[pidsStrings.length + 1];
        pids[0] = rootPid;
        for (int i = 0; i < pidsStrings.length; i++) {
            pids[i+1] = Integer.valueOf(pidsStrings[i].trim());
        }
        return pids;
    }

    /**
     * Get the stat for a given pid
     * @param pid                   The process pid
     * @return                      A ProcPidStat object
     */
    public static ProcPidStat stat(int pid) throws IOException {
        String statLine = OSUtils.exec(new String[]{"cat", "/proc/" + pid + "/stat"});
        return parseProcPidStat(statLine);
    }

    /**
     * Get the stat for the given pid and all of its children.
     * @param pids                  The pids of the process tree
     * @return                      An array of ProcPidStat objects
     */
    public static ProcPidStat[] stat(int[] pids) throws IOException {
        ProcPidStat[] stats = new ProcPidStat[pids.length];
        for (int i = 0; i < pids.length; i++) {
            stats[i] = stat(pids[i]);
        }
        return stats;
    }

    /**
     * Parse one line of /proc/pid/stat output.
     * See http://man7.org/linux/man-pages/man5/proc.5.html for format reference.
     * @param statLine              The stat output line for a given process
     * @return                      A ProcPidStat object
     */
    protected static ProcPidStat parseProcPidStat(String statLine) throws RuntimeException {
        String[] splits = statLine.split(" ");
        if (splits.length < 44) {
            throw new RuntimeException("Unable to parse stat line: `" + statLine + "` as it is too short");
        }
        try {
            int pid = Integer.parseInt(splits[0]);
            long utime = Long.parseLong(splits[13]);
            long stime = Long.parseLong(splits[14]);
            long vsize = Long.parseLong(splits[22]);
            long rss = Long.parseLong(splits[23]);

            return new ProcPidStat(pid, utime, stime, rss, vsize);
        }
        catch (NumberFormatException e) {
            throw new RuntimeException("Unable to parse stat line: `" + statLine + "`");
        }
    }

    public static ProcStat stat() throws IOException {
        String output = OSUtils.exec(new String[]{"cat", "/proc/stat"});
        return parseProcStat(output);
    }

    protected static ProcStat parseProcStat(String output) {
        String[] lines = output.split("\n");
        if (!lines[0].startsWith("cpu")) {
            throw new RuntimeException("Unexpected first line in output of /proc/stat:\n" + output);
        }

        String[] cols = lines[0].split(" +");
        if (cols.length < 10) {
            throw new RuntimeException("Unable to parse cpu line in output of /proc/stat:\n" + lines[0]);
        }

        String cpu = cols[0];
        long userTicks = Long.parseLong(cols[1]);
        long niceTicks = Long.parseLong(cols[2]);
        long systemTicks = Long.parseLong(cols[3]);
        long idleTicks = Long.parseLong(cols[4]);
        long ioWaitTicks = Long.parseLong(cols[5]);
        long irqTicks = Long.parseLong(cols[6]);
        long softIrqTicks = Long.parseLong(cols[7]);
        long stealTicks = Long.parseLong(cols[8]);
        return new ProcStat(cpu, userTicks, systemTicks, niceTicks, idleTicks, ioWaitTicks, irqTicks, softIrqTicks, stealTicks);
    }

    public static ProcPidIO io(int pid) throws IOException {
        String output = OSUtils.exec(new String[]{"cat", "/proc/" + pid + "/io"});
        return parsePidIO(output);
    }

    public static ProcPidIO[] io(int[] pids) throws IOException {
        ProcPidIO[] ios = new ProcPidIO[pids.length];
        for (int i = 0; i < pids.length; i++) {
            ios[i] = io(pids[i]);
        }
        return ios;
    }

    protected static ProcPidIO parsePidIO(String output) {
        String[] lines = output.split("\n");
        if (lines.length < 7) {
            throw new RuntimeException("Unable to parse output of /proc/pid/io:\n" + output);
        }

        long rchar = -1;
        long wchar = -1;
        long readBytes = -1;
        long writeBytes = -1;

        for (String line: lines) {
            if ("".equals(line)) continue;
            String[] cols = line.split(": +");
            if (cols.length < 2) {
                throw new RuntimeException("Unable to parse line in output of /proc/pid/io:\n" + line);
            }
            String key = cols[0];
            switch (key) {
                case "rchar":
                    rchar = Long.parseLong(cols[1]);
                    break;
                case "wchar":
                    wchar = Long.parseLong(cols[1]);
                    break;
                case "read_bytes":
                    readBytes = Long.parseLong(cols[1]);
                    break;
                case "write_bytes":
                    writeBytes = Long.parseLong(cols[1]);
                    break;
            }
        }

        if (rchar < 0 || wchar < 0 || readBytes < 0 || writeBytes < 0) {
            throw new RuntimeException("Missing field in output of /proc/pid/io:\n" + output);
        }

        return new ProcPidIO(rchar, wchar, readBytes, writeBytes);
    }

    public static ProcNetIO netio() throws IOException {
        String outputProcNetDev = OSUtils.exec(new String[]{"cat", "/proc/net/dev"});
        String outputSysClassNet = OSUtils.exec(new String[]{"ls", "-l", "/sys/class/net"});
        return parseNetIO(outputProcNetDev, outputSysClassNet);
    }

    protected static ProcNetIO parseNetIO(String outputProcNetDev, String outputSysClassNet) {
    	Set<String> realDevices = parseRealDevices(outputSysClassNet);
    	String[] lines = outputProcNetDev.split("\n");
        if (!lines[0].startsWith("Inter-|")) {
            throw new RuntimeException("Unexpected first line in output of /proc/dev/net:\n" + outputProcNetDev);
        }

        if (lines.length < 2) {
            throw new RuntimeException("Not enough lines in output of /proc/dev/net");
        }

        long rxBytes = 0;
        long txBytes = 0;

        for(int i=2; i < lines.length; i++) {
            String[] cols = lines[i].trim().split(" +"); //Trim leading spaces

            if(cols.length == 17 && realDevices.contains(cols[0])) {
                rxBytes += Long.parseLong(cols[1]);
                txBytes += Long.parseLong(cols[9]);
            }
        }

        return new ProcNetIO(rxBytes, txBytes);
	}

	protected static Set<String> parseRealDevices(String outputSysClassNet) {
		Set<String> realDevices = new HashSet<>();
		String[] lines = outputSysClassNet.split("\n");
		
		for(String line : lines) {
			String[] cols = line.split(" +");
			
			if(cols.length == 11 && !cols[10].startsWith("../../devices/virtual/net/")) {
				realDevices.add(cols[8]+":");
			}
		}
		return realDevices;
	}

	public static class ProcPidStat {
        public final int pid;
        public final long userTicks;        // number of ticks in user mode
        public final long systemTicks;      // number of ticks in kernel mode
        public final long rssPages;         // number of pages
        public final long vMemBytes;        // virtual memory in bytes

        public ProcPidStat(int pid, long userTicks, long systemTicks, long rssPages, long vMemBytes) {
            this.pid = pid;
            this.userTicks = userTicks;
            this.systemTicks = systemTicks;
            this.rssPages = rssPages;
            this.vMemBytes = vMemBytes;
        }
    }

    public static class ProcStat {
        public final String cpu;
        public final long userTicks;
        public final long systemTicks;
        public final long niceTicks;
        public final long idleTicks;
        public final long ioWaitTicks;
        public final long irqTicks;
        public final long softIrqTicks;
        public final long stealTicks;

        public ProcStat(String cpu, long userTicks, long systemTicks, long niceTicks, long idleTicks, long ioWaitTicks, long irqTicks, long softIrqTicks, long stealTicks) {
            this.cpu = cpu;
            this.userTicks = userTicks;
            this.systemTicks = systemTicks;
            this.niceTicks = niceTicks;
            this.idleTicks = idleTicks;
            this.ioWaitTicks = ioWaitTicks;
            this.irqTicks = irqTicks;
            this.softIrqTicks = softIrqTicks;
            this.stealTicks = stealTicks;
        }

        public long getActiveTicks() {
            return userTicks + systemTicks + niceTicks + stealTicks + irqTicks + softIrqTicks;
        }

        public long getTotalTicks() {
            return getActiveTicks() + idleTicks + ioWaitTicks;
        }
    }

    public static class ProcPidIO {
        public final long rchar;            // bytes read, potentially from page cache
        public final long wchar;            // bytes written, potentially to page cache
        public final long readBytes;        // bytes actually read from block storage
        public final long writeBytes;       // bytes actually written to block storage

        public ProcPidIO(long rchar, long wchar, long readBytes, long writeBytes) {
            this.rchar = rchar;
            this.wchar = wchar;
            this.readBytes = readBytes;
            this.writeBytes = writeBytes;
        }
    }
    
    public static class ProcNetIO {
        public final long rxBytes;          // bytes received
        public final long txBytes;          // bytes transmitted

        public ProcNetIO(long rxBytes, long txBytes) {
            this.rxBytes = rxBytes;
            this.txBytes = txBytes;
        }
    }

    public static class ProcSmaps {
        public long size;
        public long rss;
        public long pss;
        public long sharedClean;
        public long sharedDirty;
        public long privateClean;
        public long privateDirty;
        public long anonymous;
        public long referenced;
        public String permission;

        public ProcSmaps(long size, long rss, long pss, long sharedClean, long sharedDirty, long privateClean, long privateDirty, long anonymous, long referenced, String permission) {
            this.size = size;
            this.rss = rss;
            this.pss = pss;
            this.sharedClean = sharedClean;
            this.sharedDirty = sharedDirty;
            this.privateClean = privateClean;
            this.privateDirty = privateDirty;
            this.anonymous = anonymous;
            this.referenced = referenced;
            this.permission = permission;
        }

        private ProcSmaps(Builder builder) {
            size = builder.size;
            rss = builder.rss;
            pss = builder.pss;
            sharedClean = builder.sharedClean;
            sharedDirty = builder.sharedDirty;
            privateClean = builder.privateClean;
            privateDirty = builder.privateDirty;
            anonymous = builder.anonymous;
            referenced = builder.referenced;
            permission = builder.permission;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public static final class Builder {
            private long size;
            private long rss;
            private long pss;
            private long sharedClean;
            private long sharedDirty;
            private long privateClean;
            private long privateDirty;
            private long anonymous;
            private long referenced;
            private String permission;

            private Builder() {
            }

            public Builder size(long val) {
                size = val;
                return this;
            }

            public Builder rss(long val) {
                rss = val;
                return this;
            }

            public Builder pss(long val) {
                pss = val;
                return this;
            }

            public Builder sharedClean(long val) {
                sharedClean = val;
                return this;
            }

            public Builder sharedDirty(long val) {
                sharedDirty = val;
                return this;
            }

            public Builder privateClean(long val) {
                privateClean = val;
                return this;
            }

            public Builder privateDirty(long val) {
                privateDirty = val;
                return this;
            }

            public Builder anonymous(long val) {
                anonymous = val;
                return this;
            }

            public Builder referenced(long val) {
                referenced = val;
                return this;
            }

            public Builder permission(String val) {
                permission = val;
                return this;
            }

            public ProcSmaps build() {
                return new ProcSmaps(this);
            }
        }
    }

    public static ProcSmaps[] smaps(int[] pids) throws IOException {
        List<ProcSmaps> smaps = new ArrayList<>();
        for (int i = 0; i < pids.length; i++) {
            smaps.addAll(Arrays.asList(smaps(pids[i])));
        }
        return smaps.toArray(new ProcSmaps[]{});
    }

    public static ProcSmaps[] smaps(int pid) throws IOException {
        String output = OSUtils.exec(new String[]{"cat", "/proc/" + pid + "/smaps"});
        return parseSmaps(output).toArray(new ProcSmaps[]{});
    }

    public static final String READ_ONLY_WITH_SHARED_PERMISSION = "r--s";
    public static final String READ_EXECUTE_WITH_SHARED_PERMISSION = "r-xs";

    public enum MemInfo {
        SIZE("Size"),
        RSS("Rss"),
        PSS("Pss"),
        SHARED_CLEAN("Shared_Clean"),
        SHARED_DIRTY("Shared_Dirty"),
        PRIVATE_CLEAN("Private_Clean"),
        PRIVATE_DIRTY("Private_Dirty"),
        REFERENCED("Referenced"),
        ANONYMOUS("Anonymous"),
        ANON_HUGE_PAGES("AnonHugePages"),
        SWAP("swap"),
        KERNEL_PAGE_SIZE("kernelPageSize"),
        MMU_PAGE_SIZE("mmuPageSize");

        private String name;

        private MemInfo(String name) {
            this.name = name;
        }

        public static MemInfo getMemInfoByName(String name) {
            String searchName = name.trim();
            for (MemInfo info : MemInfo.values()) {
                if (info.name.trim().equalsIgnoreCase(searchName)) {
                    return info;
                }
            }
            return null;
        }
    }

    private static final Pattern ADDRESS_PATTERN = Pattern
            .compile("([[a-f]|(0-9)]*)-([[a-f]|(0-9)]*)(\\s)*([rxwps\\-]*)");
    private static final Pattern MEM_INFO_PATTERN = Pattern
            .compile("(^[A-Z].*):[\\s ]*(.*)");

    private static final String KB = "kB";

    protected static List<ProcSmaps> parseSmaps(String output) {
        String[] lines = output.split("\n");

        List<ProcSmaps> res = new ArrayList<>();
        ProcSmaps.Builder builder = null;

        for (String line : lines) {
            line = line.trim();
            Matcher address = ADDRESS_PATTERN.matcher(line);
            if (address.find()) {
                String permission = address.group(4).trim();
                if (builder != null) {
                    res.add(builder.build());
                }
                builder = ProcSmaps.newBuilder().permission(permission);
                continue;
            }
            Matcher memInfo = MEM_INFO_PATTERN.matcher(line);
            if (memInfo.find()) {
                String key = memInfo.group(1).trim();
                String value = memInfo.group(2).replace(KB, "").trim();

                int val = 0;
                try {
                    val = Integer.parseInt(value.trim());
                } catch (NumberFormatException ne) {
                    continue;
                }

                MemInfo info = MemInfo.getMemInfoByName(key);
                if (builder == null || info == null) {
                    continue;
                }

                switch (info) {
                    case SIZE:
                        builder.size(val);
                        break;
                    case RSS:
                        builder.rss(val);
                        break;
                    case PSS:
                        builder.pss(val);
                        break;
                    case SHARED_CLEAN:
                        builder.sharedClean(val);
                        break;
                    case SHARED_DIRTY:
                        builder.sharedDirty(val);
                        break;
                    case PRIVATE_CLEAN:
                        builder.privateClean(val);
                        break;
                    case PRIVATE_DIRTY:
                        builder.privateDirty(val);
                        break;
                    case REFERENCED:
                        builder.referenced(val);
                        break;
                    case ANONYMOUS:
                        builder.anonymous(val);
                        break;
                    default:
                        break;
                }
            }
        }

        if (builder != null) {
            res.add(builder.build());
        }

        return res;
    }
}
