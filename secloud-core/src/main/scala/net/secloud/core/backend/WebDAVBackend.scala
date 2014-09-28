package net.secloud.core.backend

import java.io._

import com.github.sardine._
import com.typesafe.config.ConfigFactory
import net.secloud.core.objects.ObjectId
import net.secloud.core.utils.StreamUtils._

class WebDAVBackend(config: WebDAVBackendConfig) extends Backend {
  private val sardine = SardineFactory.begin(config.username, config.password)

  def init(): Unit = {
    if (exists("/")) throw new Exception(s"Directory ${absoluteUrl("/")} already exists")

    createDirectory("/")
    createDirectory("/objects/")
  }

  def wipe(): Unit = {
    deleteDirectory("/")
  }

  def headId: ObjectId =
    if (exists("/HEAD")) ObjectId(new String(readFileBytes("/HEAD"), "ASCII"))
    else ObjectId.empty
  def headId_=(id: ObjectId): Unit =
    writeFileBytes("/HEAD", id.hex.getBytes("ASCII"))

  def has(id: ObjectId): Boolean = {
    exists(directoryUrl(id)) && exists(fileUrl(id))
  }

  def put(id: ObjectId, input: InputStream): Unit = {
    if (has(id)) throw new Exception(s"Object ${id.hex} already exists")

    ensureDirectory(directoryUrl(id))
    writeFile(fileUrl(id), input)
  }

  def get(id: ObjectId, output: OutputStream): Unit = {
    if (!has(id)) throw new Exception(s"Object ${id.hex} does not exist")

    readFile(fileUrl(id), output)
  }

  private def directoryUrl(id: ObjectId) = "/objects/%s/".format(id.hex.take(2))
  private def fileUrl(id: ObjectId) = "/objects/%s/%s".format(id.hex.take(2), id.hex.drop(2))
  private def absoluteUrl(relativeUrl: String) = "https://" + "%s/%s/%s".format(config.hostname, config.baseUrl, relativeUrl).replaceAll("/{2,}", "/").stripPrefix("/")

  private def exists(url: String) = sardine.exists(absoluteUrl(url))
  private def createDirectory(url: String) = sardine.createDirectory(absoluteUrl(url))
  private def deleteDirectory(url: String) = sardine.delete(absoluteUrl(url))
  private def ensureDirectory(url: String) = if (!sardine.exists(absoluteUrl(url))) sardine.createDirectory(absoluteUrl(url))
  private def writeFile(url: String, input: InputStream) = sardine.put(absoluteUrl(url), input)
  private def readFile(url: String, output: OutputStream) = using(sardine.get(absoluteUrl(url)))(input ⇒ pipeStream(input, output))
  private def writeFileBytes(url: String, bytes: Array[Byte]) = writeFile(url, new ByteArrayInputStream(bytes))
  private def readFileBytes(url: String) = {
    val stream = new ByteArrayOutputStream()
    readFile(url, stream)
    stream.toByteArray
  }
}

case class WebDAVBackendConfig(hostname: String, baseUrl: String, username: String, password: String)
object WebDAVBackendConfig {
  lazy val raw = ConfigFactory.load()

  def apply(): WebDAVBackendConfig = new WebDAVBackendConfig(
    raw.getString("secloud.webdav.hostname"),
    raw.getString("secloud.webdav.base-url"),
    raw.getString("secloud.webdav.username"),
    raw.getString("secloud.webdav.password"))
}
