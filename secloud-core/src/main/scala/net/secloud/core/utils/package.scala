package net.secloud.core

import java.io._
import scala.language.implicitConversions

package object utils {
  implicit def inputStreamToStreamReader(stream: InputStream): StreamReader = new BinaryStreamReader(stream)
  implicit def outputStreamToStreamWriter(stream: OutputStream): StreamWriter = new BinaryStreamWriter(stream)
}
