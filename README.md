# BABAR: a profiler for large-scale distributed applications

Babar is a profiler for java applications developped to **make profiling large-scale distributed applications easier**.

Babar registers metrics about **memory, cpu, garbage collection usage, as well as method calls** in each individual JVM and then aggregate them over the entire application to produce ready-to-use graphs of the resources-usage and method calls (as flame-graphs) of the program as shown in the screenshots below:

![memory-cpu](/babar-doc/memory-cpu.png)
![traces](/babar-doc/traces.png)

Currently babar is designed to **profile YARN applications such as Spark Scalding or Hive jobs,** but could be extended in order to profile other types of applications.

## How it works

Babar is composed of two main components:
1. **babar-agent**
2. **babar-processor**

The **babar agent** is a `java-agent` program. An agent is a jar that can be attached to a JVM in order to intrument this JVM. The agent fecthes, at regular interval, information on the resource comsumption and logs the resulting metrics in a plain text file named `babar.log` inside the YARN log directory. YARN's log aggregation at the end of the application them combine all the executors logs into a single log file on HDFS.

The **babar-processor** is the piece of software responsible for parsing the aggregated log file from the YARN application and aggregating the metrics found in them to produce the graphs. the logs are parsed as streams which allows the **babar-processor** to aggregate large logs files (dozens of GB) wihtout needing to load them in memory entirely at once.

Once the **babar-processor** has run, a new directory is created containing two HTML files containing the graphs (memory, CPU usage, GC usage, executor counts, flame-graphs,...).

## babar-agent

the **babar-agent** instuments the JVM to register and log the resources-usage metrics. It is a standard `java-agent` component (see the [instrumentation API doc](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html) for more information).

### instrumenting the JVM with the agent

In order to add the agent to a JVM, add the following arguments to the java command line used to start you application:

```
 -javaagent:/path/to/babar-agent.jar=StackTraceProfiler[profilingMs=100,reportingMs=60000],MemoryProfiler[profilingMs=5000,reservedMB=1024],CPUTimeProfiler[profilingMs=5000]
```

You will need to replace `/path/to/babar-agent.jar` with the actual path of the agent jar on your system. This jar must be locally accessible to your JVM (i.e. distributed on all your YARN nodes).

the profilers can be added and configured using this command line. The profilers and their configuration are described bellow.

### Available profilers

3 profilers are available:

- `CPUTimeProfiler`: this profiler registers and logs CPU usage and GC activity metrics at a regular interval. This interval can be configured using the `profilingMs` option in its arguments (e.g. `CPUTimeProfiler[profilingMs=5000]` will add the profiler and make it register metrics every 5 seconds)

- `MemoryProfiler`: this profiler registers metrics about memory (heapand off-heap used and committed memory) as well as reserved memory for the containers. The frequency of the profiling can be adjusted with `profilingMs`, and the amount of reserved memory for the executor can be indicated with `reservedMB`

- `StackTraceProfiler`: This profilers registers the stach traces of all `RUNNABLE` threads at regular intervals (the `profilingMs` options) and logs them at another interval (the `reportingMs` option) in order to aggregate multiple traces before logging them to save space in the logs. The traces are always logged at the JVM shutdown so one can set the repoting interval very high in order to save the most space in the logs if they are not interested in having traces logged in case the JVM is killed or fails.

## babar-processor

The **babar-processor** is the piece of software that parses the logs and aggregates the metrics into graphs.

### Usage

The processor needs to parse the application log aggregated by YARN, either from HDFS or from a local log file that has been fecthed using the following command (replace the application id with yours):

```
yarn logs --applicationId application_1514203639546_124445
```

To run the **babar-processor**, the following command can be used:

```
java -jar /perso/babar/babar-processor/target/babar-processor-1.0-SNAPSHOT.jar -l myAppLog.log
```

The processor accepts the following arguments:

```
  -l, --log-file  <arg>           the log file to open (REQUIRED)
  -c, --containers  <arg>         if set, only metrics of containers matching
                                  these prefixes are aggregated
                                  (comma-separated)
  -o, --output-dir  <arg>         path of the output dir (default: ./output)
  -t, --time-precision  <arg>     time precision (in ms) to use in aggregations
                                  (default: 10000)
  -m, --traces-min-ratio  <arg>   min ratio of trace samples 
                                  to show trace in graph
                                  (default: 0.001)
  -p, --traces-prefixes  <arg>    if set, traces will be aggregated only from
                                  methods matching the prefixes
                                  (comma-separated, eg: org.mygroup)
```

In the output dir (by default `./output`), two HTML files containing the graph will be generated: `memory-cpu.html` and `traces.html`.