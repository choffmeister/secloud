package net.secloud

import java.awt._
import java.awt.image._
import javax.swing._

object ImageHelper {
  def loadImage(fileName: String): Image =
    new ImageIcon(getClass.getClassLoader.getResource(fileName)).getImage

  def padCenterImage(image: Image, w: Int, h: Int, x: Int = 0, y: Int = 0): BufferedImage = {
    val res = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g = res.getGraphics
    g.drawImage(image, (w - image.getWidth(null)) / 2 + x, (h - image.getHeight(null)) / 2 + y, null)
    g.dispose()
    res
  }
}
