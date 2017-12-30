package com.bhnte.babar.processor

import java.io.{BufferedReader, InputStreamReader}

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}

object FSUtils {

  def readAsStream(path: Path): Stream[String] = {
    val fs = FileSystem.get(path.toUri, new Configuration())

    val in = fs.open(path)
    val buf = new BufferedReader(new InputStreamReader(in))
    Stream
      .cons(buf.readLine, Stream.continually(buf.readLine))
      .takeWhile(_ != null)
  }

  def copyFromResourceWithData(resource: String, dest: Path, dataAsString: String): Unit = {
    val in = getClass.getClassLoader.getResourceAsStream(resource)
    val buf = new BufferedReader(new InputStreamReader(in))
    val content = Stream
      .cons(buf.readLine, Stream.continually(buf.readLine))
      .takeWhile(_ != null)
      .map(_.replace("{{DATA}}", dataAsString))
      .mkString("\n")
    writeToFile(content, dest)
  }

  def writeToFile(content: String, path: Path): Unit = {
    val fs = FileSystem.get(path.toUri, new Configuration())

    val out = fs.create(path)
    out.writeBytes(content)
    out.close()
  }

}
