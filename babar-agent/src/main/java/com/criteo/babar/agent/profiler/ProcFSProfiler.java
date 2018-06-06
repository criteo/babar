package com.criteo.babar.agent.profiler;

import com.criteo.babar.agent.config.AgentConfig;
import com.criteo.babar.agent.profiler.utils.JVMUtils;
import com.criteo.babar.agent.profiler.utils.OSUtils;
import com.criteo.babar.agent.profiler.utils.ProcFSUtils;
import com.criteo.babar.agent.reporter.Reporter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Profiles the CPU used by the job
 */
public class ProcFSProfiler extends SamplingProfiler {

    // when using 'yarn.nodemanager.container-monitor.procfs-tree.smaps-based-rss.enabled', the following
    // permissions are not considered when computing resident memory
    private final static String[] SMAPS_YARN_PERMISSIONS_FILTER = new String[]{"r--s", "r-xs"};

    private final AtomicLong prevHostTotalCpuTicks = new AtomicLong(0L);
    private final AtomicLong prevHostActiveCpuTicks = new AtomicLong(0L);
    private final AtomicLong prevUserCpuTicks = new AtomicLong(0L);
    private final AtomicLong prevSystemCpuTicks = new AtomicLong(0L);
    private final AtomicLong prevReadBytes = new AtomicLong(0L);
    private final AtomicLong prevWriteBytes = new AtomicLong(0L);
    private final AtomicLong prevRChar = new AtomicLong(0L);
    private final AtomicLong prevWChar = new AtomicLong(0L);
    private final AtomicLong prevRxBytes = new AtomicLong(0L);
    private final AtomicLong prevTxBytes = new AtomicLong(0L);

    private int pid;
    private long pageSizeBytes;

    public ProcFSProfiler(AgentConfig agentConfig, Reporter reporter) {
        super(agentConfig, reporter);

        if (OSUtils.getOS() != OSUtils.OPERATING_SYSTEM.LINUX) {
            // make sure we are on a compatible system
            throw new RuntimeException("ProcFSProfiler is only available on linux");
        }

        try {
            this.pageSizeBytes = OSUtils.getPageSizeBytes();
            this.pid = JVMUtils.getPID();
        }
        catch (IOException | IllegalAccessException e) {
            throw new RuntimeException("Unable to initialize ProcFSProfiler", e);
        }

    }

    @Override
    public void start(long startTimeMs) throws Exception {
        int[] pids = ProcFSUtils.getChildrenPids(pid);
        ProcFSUtils.ProcPidStat[] processesStats = ProcFSUtils.stat(pids);
        ProcFSUtils.ProcStat cpuStats = ProcFSUtils.stat();
        ProcFSUtils.ProcPidIO[] ios = ProcFSUtils.io(pids);
        ProcFSUtils.ProcNetIO netIO = ProcFSUtils.netio();

        long userTicks = 0L;
        long systemTicks = 0L;
        long readBytes = 0L;
        long writeBytes = 0L;

        for (ProcFSUtils.ProcPidStat stat: processesStats) {
            userTicks += stat.userTicks;
            systemTicks += stat.systemTicks;
        }
        for (ProcFSUtils.ProcPidIO io: ios) {
            readBytes += io.readBytes;
            writeBytes += io.writeBytes;
        }

        this.prevUserCpuTicks.set(userTicks);
        this.prevSystemCpuTicks.set(systemTicks);
        this.prevHostTotalCpuTicks.set(cpuStats.getTotalTicks());
        this.prevHostActiveCpuTicks.set(cpuStats.getActiveTicks());
        this.prevReadBytes.set(readBytes);
        this.prevWriteBytes.set(writeBytes);

        this.prevRxBytes.set(netIO.rxBytes);
        this.prevTxBytes.set(netIO.txBytes);
    }

    @Override
    public void stop(long stopTimeMs, long deltaLastSampleMs) throws Exception {
    }

    @Override
    public void sample(long sampleTimeMs, long deltaLastSampleMs) throws Exception {

        int[] pids = ProcFSUtils.getChildrenPids(pid);
        ProcFSUtils.ProcPidStat[] processesStats = ProcFSUtils.stat(pids);
        ProcFSUtils.ProcStat cpuStats = ProcFSUtils.stat();
        ProcFSUtils.ProcPidIO[] ios = ProcFSUtils.io(pids);
        ProcFSUtils.ProcSmaps[] smaps = ProcFSUtils.smaps(pids, SMAPS_YARN_PERMISSIONS_FILTER);
        ProcFSUtils.ProcNetIO netIO = ProcFSUtils.netio();

        long rssPages = 0L;
        long vMemBytes = 0L;
        long smapsCorrectedRssBytes = 0L;          // anonymous pages are pages not backed by a file on disk
        long userTicks = 0L;
        long systemTicks = 0L;
        long hostTotalTicks = cpuStats.getTotalTicks();
        long hostActiveTicks = cpuStats.getActiveTicks();
        long readBytes = 0L;
        long writeBytes = 0L;
        long rchar = 0L;
        long wchar = 0L;
        long rxBytes = netIO.rxBytes;
        long txBytes = netIO.txBytes;

        for (ProcFSUtils.ProcPidStat stat: processesStats) {
            rssPages += stat.rssPages;
            vMemBytes += stat.vMemBytes;
            userTicks += stat.userTicks;
            systemTicks += stat.systemTicks;
        }
        for (ProcFSUtils.ProcPidIO io: ios) {
            readBytes += io.readBytes;
            writeBytes += io.writeBytes;
            rchar += io.rchar;
            wchar += io.wchar;
        }
        for (ProcFSUtils.ProcSmaps s: smaps) {
            smapsCorrectedRssBytes += Math.min(s.sharedDirty, s.pss) + s.privateDirty + s.privateClean;
        }

        double userTicksDelta = userTicks - prevUserCpuTicks.getAndSet(userTicks);
        double systemTicksDelta = systemTicks - prevSystemCpuTicks.getAndSet(systemTicks);
        double treeTicksDelta = userTicksDelta + systemTicksDelta;
        double hostTotalTicksDelta = hostTotalTicks - prevHostTotalCpuTicks.getAndSet(hostTotalTicks);
        double hostActiveTicksDelta = hostActiveTicks - prevHostActiveCpuTicks.getAndSet(hostActiveTicks);
        double deltaReadBytes = readBytes - prevReadBytes.getAndSet(readBytes);
        double deltaWriteBytes = writeBytes - prevWriteBytes.getAndSet(writeBytes);
        double deltaRChar = rchar - prevRChar.getAndSet(rchar);
        double deltaWChar = wchar - prevWChar.getAndSet(wchar);
        double deltaRxBytes = rxBytes - prevRxBytes.getAndSet(rxBytes);
        double deltaTxBytes = txBytes - prevTxBytes.getAndSet(txBytes);

        double sec = deltaLastSampleMs / 1000D;
        double readBytesPerSec = deltaReadBytes / sec;
        double writeBytesPerSec = deltaWriteBytes / sec;
        double rCharPerSec = deltaRChar / sec;
        double wCharPerSec = deltaWChar / sec;
        double rxBytesPerSec = deltaRxBytes / sec;
        double txBytesPerSec = deltaTxBytes / sec;

        double treeCpuTime = treeTicksDelta * OSUtils.getJiffyLengthInMillis();
        double userCpuLoad = userTicksDelta / hostTotalTicksDelta;
        double systemCpuLoad = systemTicksDelta / hostTotalTicksDelta;
        double hostCpuLoad = hostActiveTicksDelta / hostTotalTicksDelta;
        double appCpuLoad = userCpuLoad + systemCpuLoad;

        reporter.reportEvent("PROC_TREE_RSS_MEMORY_BYTES", "", rssPages * (double)this.pageSizeBytes, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_VIRTUAL_MEMORY_BYTES", "", (double)vMemBytes, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_USER_MODE_CPU_LOAD", "", userCpuLoad, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_KERNEL_MODE_CPU_LOAD", "", systemCpuLoad, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_CPU_LOAD", "", appCpuLoad, sampleTimeMs);
        reporter.reportEvent("PROC_HOST_CPU_LOAD", "", hostCpuLoad, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_CPU_TIME", "", treeCpuTime, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_READ_BYTES", "", deltaReadBytes, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_WRITE_BYTES", "", deltaWriteBytes, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_RCHAR", "", deltaRChar, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_WCHAR", "", deltaWChar, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_READ_BYTES_PER_SEC", "", readBytesPerSec, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_WRITE_BYTES_PER_SEC", "", writeBytesPerSec, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_RCHAR_PER_SEC", "", rCharPerSec, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_WCHAR_PER_SEC", "", wCharPerSec, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_RXBYTES_PER_SEC", "", rxBytesPerSec, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_TXBYTES_PER_SEC", "", txBytesPerSec, sampleTimeMs);
        reporter.reportEvent("PROC_TREE_SMAPS_CORRECTED_RSS_BYTES", "", (double)smapsCorrectedRssBytes, sampleTimeMs);
    }
}
