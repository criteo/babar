package com.criteo.babar.processor

import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.{Charset, StandardCharsets}
import java.util.Collections

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}

object HDFSUtils {

  private def readAsStream(fs: FileSystem, path: Path): Stream[String] = {
    val in = fs.open(path)
    val buf = new BufferedReader(new InputStreamReader(in))
    Stream
      .cons(buf.readLine, Stream.continually(buf.readLine))
      .takeWhile(_ != null)
  }

  def readAsStreamWithProgressBar(filePath: String): Stream[String] = {
    val path = new Path(filePath)
    val fs = FileSystem.get(path.toUri, new Configuration())
    val totalByteSize = fs.getFileStatus(path).getLen
    val charset = Charset.forName("UTF-8")

    var readBytes = 0
    var percent = 0
    var nextMilestone = 1

    readAsStream(fs, path)
      .map{ line =>
        readBytes += line.getBytes(charset).length
        percent = Math.ceil(readBytes.toDouble / totalByteSize * 100).toInt

        if (percent >= nextMilestone) {
          nextMilestone = Math.floor(percent + 1).toInt
          val s = new StringBuilder()
            .append('\r')
            .append(f"$percent%3d%% [")
            .append(String.join("", Collections.nCopies(percent, "=")))
            .append('>')
            .append(String.join("", Collections.nCopies(100 - percent, " ")))
            .append(']')
          System.out.print(s)
        }
        line
      }
  }
}
