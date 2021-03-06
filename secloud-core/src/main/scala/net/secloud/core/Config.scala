package net.secloud.core

import java.io._
import net.secloud.core.crypto._
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.util.matching.Regex

case class Config(
  asymmetricKey: AsymmetricAlgorithmInstance,
  symmetricAlgorithm: SymmetricAlgorithm,
  symmetricAlgorithmKeySize: Int,
  ignore: List[Regex])

object Config {
  lazy val raw = ConfigFactory.load()

  def apply(): Config = new Config(
    loadAsymmetricKey(new File(raw.getString("secloud.key.path"))),
    resolveSymmetricAlgorithm(raw.getString("secloud.encryption.algorithm.name")),
    raw.getInt("secloud.encryption.algorithm.keysize"),
    "/.secloud$".r :: "/.secloud/".r :: raw.getStringList("secloud.ignore").toList.map(_.r))

  private def loadAsymmetricKey(keyPath: File): AsymmetricAlgorithmInstance = {
    try {
      readFile(keyPath)(fs ⇒ RSA.loadFromPEM(fs))
    } catch {
      case e: Throwable ⇒ throw new Exception(s"Could not load private key $keyPath", e)
    }
  }

  private def resolveSymmetricAlgorithm(str: String): SymmetricAlgorithm = str match {
    case "aes" ⇒ AES
    case _ ⇒ throw new Exception(s"Unknown symmetric algorithm")
  }

  private def readFile[T](f: File)(inner: InputStream ⇒ T): T = {
    val fs = new FileInputStream(f)
    try {
      inner(fs)
    } finally {
      fs.close()
    }
  }
}
