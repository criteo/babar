package com.criteo.babar.processor

import java.io.{BufferedReader, FileInputStream, FileOutputStream, FileReader, InputStream, InputStreamReader, OutputStreamWriter}

object IOUtils {

  private val REPLACE_PATTERN = "{{DATA}}"

  def copyFromResources(inputPath: String, outputPath: String, data: String): Unit = {
    var is: InputStream = null
    var isr: InputStreamReader = null
    var in: BufferedReader = null

    var fos: FileOutputStream = null
    var out: OutputStreamWriter = null

    try {
      is = getClass.getClassLoader.getResourceAsStream(inputPath)
      isr = new InputStreamReader(is, "UTF-8")
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
      if (is != null) is.close()

      if (out != null) out.close()
      if (fos != null) fos.close()

    }
  }

}
