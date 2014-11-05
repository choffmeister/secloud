package de.choffmeister.sbt

import sbt._
import scala.xml.{ Node, XML }
import scala.xml.dtd.{ DocType, PublicID }

sealed trait PListValue

case object PListNull extends PListValue
case class PListBoolean(value: Boolean) extends PListValue
case class PListString(value: String) extends PListValue
case class PListArray(list: List[PListValue]) extends PListValue
case class PListDict(map: Map[String, PListValue]) extends PListValue

object PListWriter {
  def write(plist: PList): Node = <plist version="1.0">{write(plist.dict)}</plist>

  def write(value: PListValue): Node = value match {
    case PListNull => ???
    case PListBoolean(value) =>
      if (value) <true/> else <false/>
    case PListString(value) =>
      <string>{value}</string>
    case PListArray(list) =>
      <array>{list.filter(_ != PListNull).map(write(_))}</array>
    case PListDict(map) =>
      <dict>{map.filter(_._2 != PListNull).map {
        case (key, value) => <key>{key}</key> ++ write(value)
      }}</dict>
  }
}

case class PList(dict: PListDict)

object PList {
  val docType = DocType("plist", PublicID("-//Apple//DTD PLIST 1.0//EN",
    "http://www.apple.com/DTDs/PropertyList-1.0.dtd"), Nil)

  def writeAsXml(plist: PList): Node = PListWriter.write(plist)

  def writeAsString(plist: PList): String = {
    val x = writeAsXml(plist)
    val w = new java.io.StringWriter()
    XML.write(w, node = x, enc = "UTF-8", xmlDecl = true, doctype = docType)
    w.toString
  }

  def writeToFile(plist: PList, file: File): Unit = IO.write(file, writeAsString(plist))

  def apply(map: (String, PListValue)*): PList = PList(map.toMap)
  def apply(map: Map[String, PListValue]): PList = PList(PListDict(map))
}


object PListDict {
  def apply(map: (String, PListValue)*): PListDict = PListDict(map.toMap)
}

object PListArray {
  def apply(list: PListValue*): PListArray = PListArray(list.toList)
}
