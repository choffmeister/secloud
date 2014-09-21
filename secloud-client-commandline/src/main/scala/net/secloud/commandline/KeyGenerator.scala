package net.secloud.commandline

import java.io._
import net.secloud.core._
import net.secloud.core.crypto._

object KeyGenerator {
  private lazy val log = org.slf4j.LoggerFactory.getLogger(getClass)

  def generate(env: Environment, strength: Int, certaincy: Int) {
    val secloudDir = new File(env.userDirectory, ".secloud")
    val filePub = new File(secloudDir, "rsa.pub")
    val filePriv = new File(secloudDir, "rsa.key")

    log.info(s"Generating a new RSA key pair at ${filePriv}")

    if (filePub.exists) throw new Exception(s"File ${filePub} already exists")
    if (filePriv.exists) throw new Exception(s"File ${filePriv} already exists")

    val rsa = RSA.generate(strength, certaincy).asInstanceOf[RSA]

    ensureDirectory(secloudDir)
    writeFile(filePriv)(s ⇒ RSA.saveToPEM(s, rsa, true))
    writeFile(filePub)(s ⇒ RSA.saveToPEM(s, rsa, false))
  }

  private def ensureDirectory(file: File) {
    if (!file.exists()) file.mkdirs()
  }

  private def writeFile(file: File)(inner: OutputStream ⇒ Any) {
    val s = new FileOutputStream(file)
    try {
      inner(s)
    } finally {
      s.close()
    }
  }
}
