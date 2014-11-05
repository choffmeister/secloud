package net.secloud

import java.awt._
import java.awt.event.{ ActionEvent, ActionListener }

object Application {
  def main(args: Array[String]): Unit = {
    val tray = SystemTray.getSystemTray

    val menu = new PopupMenu("Secloud")
    val menuItemExit = new MenuItem("Exit")
    menu.add(menuItemExit)

    val image = loadTrayIcon(tray)
    val trayIcon = new TrayIcon(image, "Secloud", menu)
    trayIcon.setImageAutoSize(false)
    tray.add(trayIcon)

    menuItemExit.addActionListener(new ActionListener {
      override def actionPerformed(p1: ActionEvent): Unit = {
        tray.remove(trayIcon)
        System.exit(0)
      }
    })
  }

  def screenScale: Double = {
    Option(Toolkit.getDefaultToolkit.getDesktopProperty("apple.awt.contentScaleFactor")) match {
      case Some(scale) ⇒ scale.asInstanceOf[Float].toDouble
      case _ ⇒
        val gd = GraphicsEnvironment.getLocalGraphicsEnvironment.getDefaultScreenDevice
        Option(gd.getClass.getDeclaredField("scale")) match {
          case Some(field) ⇒
            field.setAccessible(true)
            field.get(gd) match {
              case i if i.isInstanceOf[Int] ⇒ i.asInstanceOf[Int].toDouble
              case _ ⇒ 1.0
            }
          case _ ⇒ 1.0
        }
    }
  }

  def loadTrayIcon(tray: SystemTray): Image = {
    screenScale match {
      case 2.0 ⇒ ImageHelper.padCenterImage(ImageHelper.loadImage("images/tray-icon@2.png"),
        tray.getTrayIconSize.width * 2, tray.getTrayIconSize.height * 2, 0, 2)
      case _ ⇒ ImageHelper.padCenterImage(ImageHelper.loadImage("images/tray-icon.png"),
        tray.getTrayIconSize.width, tray.getTrayIconSize.height, 0, 0)
    }
  }
}