package de.choffmeister.secloud.core

import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.binary.Base64

case class ObjectId(bytes: Array[Byte]) {
  override def equals(other: Any): Boolean = other match {
    case ObjectId(otherBytes) => bytes.deep == otherBytes.deep
    case _ => false
  }
  
  /**
   * Simple hash value taking the at most the first 4 bytes into account.
   */
  override def hashCode(): Int = bytes.take(4).toList match {
    case Nil => 0
    case List(a) => a
    case List(a, b) => a | b << 8
    case List(a, b, c) => a | b << 8 | c << 16
    case List(a, b, c, d) => a | b << 8 | c << 16 | d << 24
  }

  def hex = Hex.encodeHexString(bytes)
  def base64 = Base64.encodeBase64String(bytes)
  
  override def toString() = hex
}

object ObjectId {
  def apply(): ObjectId = ObjectId(Array.empty[Byte])
  def apply(hex: String): ObjectId = ObjectId(Hex.decodeHex(hex.toCharArray()))
}