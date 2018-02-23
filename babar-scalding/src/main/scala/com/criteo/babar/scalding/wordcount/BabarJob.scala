package com.criteo.babar.scalding.wordcount

import cascading.flow.FlowStep
import com.twitter.scalding.filecache.DistributedCacheFile
import com.twitter.scalding.{Args, Job}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.mapred.JobConf

import collection.JavaConverters._

/**
  * Example instrumentation of a Scalding job to distribute the agent jar, add the
  * agent config to the executors' java options and run the job.
  * @param args     Scalding arguments
  */
class BabarJob(args: Args) extends Job(args) {

  /**
    * Copy the local agent jar to the distributed file system.
    * The local agent jar path has to be passed in the arguments with `--agentJar`.
    * @return     The path of the agent jar in the containers' work dir.
    */
  private def distributeAgentJar(): String = {
    val localAgentPath = new Path(args("agentJar"))
    val fs = FileSystem.get(localAgentPath.toUri, new Configuration())
    val user = System.getProperty("user.name")
    val distAgentPath = new Path(s"/user/$user/babar-agent.jar")
    // copy local agent jar to distributed filesystem
    fs.copyFromLocalFile(localAgentPath, distAgentPath)
    // return path of the agent in the containers
    DistributedCacheFile(distAgentPath.toUri).path
  }

  private val agentPathInContainers = distributeAgentJar()

  /**
    * Build the agent argument string using values from the job conf.
    * @param mapOrReduce    "map" if step is a map, "reduce" otherwise
    * @param jobConf        the job conf
    * @return               agent arguments string
    */
  private def agentOpts(mapOrReduce: String, jobConf: JobConf): String = {
    val reservedMemoryMB = mapOrReduce match {
      case "map" => jobConf.getMemoryForMapTask
      case "reduce" => jobConf.getMemoryForReduceTask
      case _ => 0
    }
    s"StackTraceProfiler[profilingMs=100,reportingMs=60000]," +
      s"MemoryProfiler[profilingMs=5000,reservedMB=$reservedMemoryMB]," +
      s"CPUTimeProfiler[profilingMs=5000]"
  }

  /**
    * Add the custom java options activating the agent in each step  of the workflow
    * @param step       the flow step object
    */
  private def addAgentConfigToStep(step: FlowStep[JobConf]): Unit = {
    step.getConfig.set("mapreduce.map.java.opts",
      s"${step.getConfig.get("mapreduce.map.java.opts", "")} -javaagent:$agentPathInContainers=${agentOpts("map", step.getConfig)}")
    step.getConfig.set("mapreduce.reduce.java.opts",
      s"${step.getConfig.get("mapreduce.reduce.java.opts", "")} -javaagent:$agentPathInContainers=${agentOpts("reduce", step.getConfig)}")
  }

  /**
    * Override the logic to run the job to first distribute the agent jar,
    * add the agent arguments to the java options of the mappers and reducers,
    * and finally run the flow.
    * @return       true if the job succeeded
    */
  override def run(): Boolean = {
    val flow = buildFlow
    flow.getFlowSteps.asScala.foreach{ step =>
      addAgentConfigToStep(step.asInstanceOf[FlowStep[JobConf]])
    }
    flow.complete() // actually run the flow
    flow.getFlowStats.isSuccessful
  }

}
