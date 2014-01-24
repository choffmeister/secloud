package net.secloud.core.objects

import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ObjectIdSpec extends Specification {
  val oh1 = ObjectId("00112233feff")
  val oh2 = ObjectId("00112233feff")
  val oh3 = ObjectId("01112233feff")
  val oh4 = ObjectId("00112233fefe")

  "ObjectHash" should {
    "properly supply hash codes" in {
      ObjectId().hashCode == 0
      ObjectId("10").hashCode == 16
      ObjectId("1020").hashCode == 8208

      oh1.hashCode === oh2.hashCode
      oh1.hashCode !== oh3.hashCode
      oh1.hashCode === oh4.hashCode
    }

    "properly compare" in {
      oh1 === oh2
      oh1 == oh2 === true
      oh1.eq(oh2) === false

      oh1 !== oh3
      oh1 == oh3 === false
      oh1.eq(oh3) === false
    }
  }
}
