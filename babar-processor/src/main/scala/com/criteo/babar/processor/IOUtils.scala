package com.criteo.babar.processor

import java.io.{BufferedReader, FileInputStream, FileOutputStream, FileReader, InputStream, InputStreamReader, OutputStreamWriter}

object IOUtils {

  private val REPLACE_PATTERN = "{{DATA}}"

  def copyFromResources(inputPath: String, outputPath: String, data: String): Unit = {

    val resource = getClass.getClassLoader.getResource(inputPath).getPath

    var fis: FileInputStream = null
    var isr: InputStreamReader = null
    var in: BufferedReader = null

    var fos: FileOutputStream = null
    var out: OutputStreamWriter = null

    try {
      fis = new FileInputStream(resource)
      isr = new InputStreamReader(fis, "UTF-8")
      in = new BufferedReader(isr)

      fos = new FileOutputStream(outputPath)
      out = new OutputStreamWriter(fos, "UTF8")

      var line = in.readLine()
      while (line != null) {
        out.write(line.replace(REPLACE_PATTERN, data))
        out.write('\n')
        line = in.readLine()
      }
    }
    finally {
      if (isr != null) isr.close()
      if (fis != null) fis.close()

      if (out != null) out.close()
      if (fos != null) fos.close()

    }
  }

}
