package com.criteo.babar.agent.profiler.utils;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ProcFSUtilsTest {

    @Test
    public void pidStat() throws Exception {
        // not available on systems other than linux
        if (!ProcFSUtils.isAvailable()) return;

        int self = JVMUtils.getPID();
        ProcFSUtils.stat(self);
    }

    @Test
    public void parseProcPidStat() throws Exception {
        String statLine = "3136 (test) S 3118 3136 3136 1026 3136 4194560 615 341 0 0 34820 24707 0 0 20 0 3 0 5545 205201408 573 18446744073709551615 93841664516096 93841664582996 140721340824240 140721340823440 139997457362109 0 0 4096 81920 0 0 0 17 0 0 0 0 0 0 93841666683496 93841666687240 93841689841664 140721340833027 140721340833098 140721340833098 140721340833756 0";
        ProcFSUtils.ProcPidStat stat = ProcFSUtils.parseProcPidStat(statLine);

        assertEquals(3136, stat.pid);
        assertEquals(34820L, stat.userTicks);
        assertEquals(24707L, stat.systemTicks);
        assertEquals(205201408L, stat.vMemBytes);
        assertEquals(573L, stat.rssPages);
    }

    @Test
    public void getChildrenPids() throws Exception {
        String output = " 3138\n 3148\n";
        int[] pids = ProcFSUtils.parsePidsOutput(1234, output);

        assertEquals(3, pids.length);
        assertEquals(1234, pids[0]);
        assertEquals(3138, pids[1]);
        assertEquals(3148, pids[2]);
    }

    @Test
    public void stat() throws Exception {
        // not available on systems other than linux
        if (!ProcFSUtils.isAvailable()) return;

        ProcFSUtils.stat();
    }

    @Test
    public void parseProcStat() throws Exception {
        String output =
                "cpu  35457856 12475 38956533 336058048 483830 0 47316 0 0 0\n" +
                "cpu0 4505936 1843 2704239 44111557 31057 0 4728 0 0 0\n" +
                "cpu1 5108968 1622 6857401 39353386 50491 0 1882 0 0 0\n" +
                "cpu2 4864999 1546 1441958 44963628 58282 0 1495 0 0 0\n" +
                "cpu3 4782936 1458 6169173 40381422 37951 0 2472 0 0 0\n" +
                "cpu4 4634132 3412 5855821 40677338 250286 0 6876 0 0 0\n" +
                "cpu5 3706231 1098 4668066 42959200 18038 0 27342 0 0 0\n" +
                "cpu6 3005570 742 1822001 46482188 20207 0 505 0 0 0\n" +
                "cpu7 4849080 750 9437871 37129326 17514 0 2012 0 0 0\n" +
                "intr 1155274207 22 2 0 0 0 0 0 0 1 0 0 0 4 0 0 0 0 1947 0 24 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 2855954 14917293 25108924 43 874 46502617 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
                "ctxt 4119743156\n" +
                "btime 1521619939\n" +
                "processes 604164\n" +
                "procs_running 2\n" +
                "procs_blocked 0\n" +
                "softirq 634584520 1 291633213 97104 31506294 14856585 0 3023377 174263195 0 119204751\n";

        ProcFSUtils.ProcStat stats = ProcFSUtils.parseProcStat(output);

        assertEquals(35457856L, stats.userTicks);
        assertEquals(12475L, stats.niceTicks);
        assertEquals(38956533L, stats.systemTicks);
        assertEquals(336058048L, stats.idleTicks);
        assertEquals(483830L, stats.ioWaitTicks);
        assertEquals(0L, stats.irqTicks);
        assertEquals(47316L, stats.softIrqTicks);
    }

    @Test
    public void io() throws Exception {
        // not available on systems other than linux
        if (!ProcFSUtils.isAvailable()) return;

        int self = JVMUtils.getPID();
        ProcFSUtils.io(self);
    }

    @Test
    public void parsePidIO() throws Exception {
        String output = "rchar: 15206255\n" +
                "wchar: 1117\n" +
                "syscr: 2449\n" +
                "syscw: 24\n" +
                "read_bytes: 880640\n" +
                "write_bytes: 36864\n" +
                "cancelled_write_bytes: 0\n";

        ProcFSUtils.ProcPidIO io = ProcFSUtils.parsePidIO(output);

        assertEquals(15206255L, io.rchar);
        assertEquals(1117L, io.wchar);
        assertEquals(880640L, io.readBytes);
        assertEquals(36864L, io.writeBytes);
    }

    @Test
    public void parseSmapsNoPermissionException() throws Exception {
        String output = "7f1ada0ca000-7f1ada19c000 rw-p 00000000 00:00 0 \n" +
                "Size:                840 kB\n" +
                "Rss:                 836 kB\n" +
                "Pss:                 836 kB\n" +
                "Shared_Clean:          0 kB\n" +
                "Shared_Dirty:          0 kB\n" +
                "Private_Clean:         0 kB\n" +
                "Private_Dirty:       836 kB\n" +
                "Referenced:          836 kB\n" +
                "Anonymous:           836 kB\n" +
                "AnonHugePages:         0 kB\n" +
                "Shared_Hugetlb:        0 kB\n" +
                "Private_Hugetlb:       0 kB\n" +
                "Swap:                  0 kB\n" +
                "SwapPss:               0 kB\n" +
                "KernelPageSize:        4 kB\n" +
                "MMUPageSize:           4 kB\n" +
                "Locked:                0 kB\n" +
                "VmFlags: rd wr mr mw me ac \n" +
                "7f1ada19d000-7f1ada29d000 r--s 00000000 00:00 0 \n" +
                "Size:               1024 kB\n" +
                "Rss:                  12 kB\n" +
                "Pss:                  12 kB\n" +
                "Shared_Clean:          0 kB\n" +
                "Shared_Dirty:          0 kB\n" +
                "Private_Clean:         0 kB\n" +
                "Private_Dirty:        12 kB\n" +
                "Referenced:           12 kB\n" +
                "Anonymous:            12 kB\n" +
                "AnonHugePages:         0 kB\n" +
                "Shared_Hugetlb:        0 kB\n" +
                "Private_Hugetlb:       0 kB\n" +
                "Swap:                  0 kB\n" +
                "SwapPss:               0 kB\n" +
                "KernelPageSize:        4 kB\n" +
                "MMUPageSize:           4 kB\n" +
                "Locked:                0 kB\n" +
                "VmFlags: rd wr mr mw me ac \n";
        List<ProcFSUtils.ProcSmaps> smaps = ProcFSUtils.parseSmaps(output);

        ProcFSUtils.ProcSmaps s = null;
        
        s = smaps.get(0);
        assertEquals("rw-p", s.permission);
        assertEquals(840, s.size);
        assertEquals(836, s.rss);
        assertEquals(836, s.pss);
        assertEquals(0, s.sharedClean);
        assertEquals(0, s.sharedDirty);
        assertEquals(0, s.privateClean);
        assertEquals(836, s.privateDirty);
        assertEquals(836, s.anonymous);
        assertEquals(836, s.referenced);

        s = smaps.get(1);
        assertEquals("r--s", s.permission);
        assertEquals(1024, s.size);
        assertEquals(12, s.rss);
        assertEquals(12, s.pss);
        assertEquals(0, s.sharedClean);
        assertEquals(0, s.sharedDirty);
        assertEquals(0, s.privateClean);
        assertEquals(12, s.privateDirty);
        assertEquals(12, s.anonymous);
        assertEquals(12, s.referenced);
    }
    
    @Test
    public void parseNetIO() throws Exception {
        String outputProcNetDev = "Inter-|   Receive                                                |  Transmit\n" + 
        		" face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed\n" + 
        		" wlan0: 179407626  151664    0    0    0     0          0         0  9802910   66528    0    0    0     0       0          0\n" + 
        		"br-62486ecab361:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0\n" + 
        		"br-fbe64819fc03:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0\n" + 
        		"  eth0: 14154021698 10146715    0    0    0     0          0      8648 523034937 5318065    0    0    0     0       0          0\n" + 
        		"    lo: 11532725   42197    0    0    0     0          0         0 11532725   42197    0    0    0     0       0          0\n" + 
        		"br-8fc5bc2e8e53:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0\n" + 
        		"br-c91c731c9737:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0\n" + 
        		"docker0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0\n" + 
        		"br-456d4d6fea94:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0\n" + 
        		"br-943b2843c6cf:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0\n";
        
        String outputSysClassNet = "insgesamt 0\n" + 
        		"lrwxrwxrwx 1 root root 0 Jun 15 16:07 br-456d4d6fea94 -> ../../devices/virtual/net/br-456d4d6fea94\n" + 
        		"lrwxrwxrwx 1 root root 0 Jun 15 16:07 br-62486ecab361 -> ../../devices/virtual/net/br-62486ecab361\n" + 
        		"lrwxrwxrwx 1 root root 0 Jun 15 16:07 br-8fc5bc2e8e53 -> ../../devices/virtual/net/br-8fc5bc2e8e53\n" + 
        		"lrwxrwxrwx 1 root root 0 Jun 15 16:07 br-943b2843c6cf -> ../../devices/virtual/net/br-943b2843c6cf\n" + 
        		"lrwxrwxrwx 1 root root 0 Jun 15 16:07 br-c91c731c9737 -> ../../devices/virtual/net/br-c91c731c9737\n" + 
        		"lrwxrwxrwx 1 root root 0 Jun 15 16:07 br-fbe64819fc03 -> ../../devices/virtual/net/br-fbe64819fc03\n" + 
        		"lrwxrwxrwx 1 root root 0 Jun 15 16:07 docker0 -> ../../devices/virtual/net/docker0\n" + 
        		"lrwxrwxrwx 1 root root 0 Jun 15 16:17 eth0 -> ../../devices/pci0000:00/0000:00:19.0/net/eth0\n" + 
        		"lrwxrwxrwx 1 root root 0 Jun 15 16:06 lo -> ../../devices/virtual/net/lo\n" + 
        		"lrwxrwxrwx 1 root root 0 Jun 15 16:06 wlan0 -> ../../devices/pci0000:00/0000:00:1c.1/0000:03:00.0/net/wlan0\n";

        ProcFSUtils.ProcNetIO io = ProcFSUtils.parseNetIO(outputProcNetDev, outputSysClassNet);

        assertEquals(14333429324L, io.rxBytes);
        assertEquals(532837847L, io.txBytes);
    }
    
    @Test
    public void parseNetIONoInterfaces() throws Exception {
        String output = "Inter-|   Receive                                                |  Transmit\n" + 
        		" face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed\n";
        
        String outputSysClassNet = "insgesamt 0\n" +  
        		"lrwxrwxrwx 1 root root 0 Jun 15 16:17 eth0 -> ../../devices/pci0000:00/0000:00:19.0/net/eth0\n" + 
        		"lrwxrwxrwx 1 root root 0 Jun 15 16:06 lo -> ../../devices/virtual/net/lo\n" + 
        		"lrwxrwxrwx 1 root root 0 Jun 15 16:06 wlan0 -> ../../devices/pci0000:00/0000:00:1c.1/0000:03:00.0/net/wlan0\n";

        ProcFSUtils.ProcNetIO io = ProcFSUtils.parseNetIO(output, outputSysClassNet);

        assertEquals(0L, io.rxBytes);
        assertEquals(0L, io.txBytes);
    }
}