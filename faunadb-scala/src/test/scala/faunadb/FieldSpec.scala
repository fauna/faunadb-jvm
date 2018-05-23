package faunadb

import faunadb.values._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FieldSpec extends AnyFlatSpec with Matchers {

  val pt1 = ObjectV("x" -> LongV(10), "y" -> LongV(110))
  val pt2 = ObjectV("x" -> LongV(20), "y" -> LongV(120))
  val pt3 = ObjectV("x" -> LongV(30), "y" -> LongV(130))
  val pt4 = ObjectV("x" -> LongV(40), "y" -> LongV(140))
  val points = ArrayV(pt1, pt2, pt3, pt4)

  val DataField = Field("data")
  val XField = Field("x").to[Long]
  val YField = Field("y").to[Long]

  it should "get field" in {
    pt1("x").get shouldBe LongV(10)
    pt1("y").get shouldBe LongV(110)

    points(0, "x").get shouldBe LongV(10)
    points(0, "y").get shouldBe LongV(110)

    pt1(XField).get shouldBe 10
    pt1(YField).get shouldBe 110
  }

  it should "collect fields" in {
    val AllXFields = Field.collect(XField)
    val AllYFields = Field.collect(YField)

    points(AllXFields).get shouldBe Seq[Long](10, 20, 30, 40)
    points(AllYFields).get shouldBe Seq[Long](110, 120, 130, 140)
  }

  it should "combine fields" in {
    val data = ObjectV("data" -> pt1)

    val X = DataField.at(XField)
    val Y = DataField.at(YField)

    data(X).get shouldBe 10
    data(Y).get shouldBe 110
  }

  it should "combine fields with index" in {
    val data = ObjectV("data" -> points)

    val Index0 = DataField(0)
    val X = Index0.at(XField)
    val Y = Index0.at(YField)

    data(X).get shouldBe 10
    data(Y).get shouldBe 110
  }

  it should "combine fields with collect" in {
    val data = ObjectV("data" -> points)

    val AllXFields = Field.collect(XField)
    val AllYFields = Field.collect(YField)

    val X = DataField.at(AllXFields)
    val Y = DataField.at(AllYFields)

    data(X).get shouldBe Seq[Long](10, 20, 30, 40)
    data(Y).get shouldBe Seq[Long](110, 120, 130, 140)
  }

  it should "zip fields" in {
    val Zip = Field.zip(XField, YField)

    pt1(Zip).get shouldBe ((10, 110))
    pt2(Zip).get shouldBe ((20, 120))
    pt3(Zip).get shouldBe ((30, 130))
    pt4(Zip).get shouldBe ((40, 140))
  }

  it should "map fields" in {
    val ToX = Field.map(v => v(XField).get)
    val ToY = Field.map(v => v(YField).get)

    pt1(ToX).get shouldBe 10
    pt1(ToY).get shouldBe 110
  }

  it should "zip fields with map" in {
    val SqLength = Field.zip(XField, YField).map(xy => xy._1*xy._1 + xy._2*xy._2)

    pt1(SqLength).get shouldBe 12200
    pt2(SqLength).get shouldBe 14800
    pt3(SqLength).get shouldBe 17800
    pt4(SqLength).get shouldBe 21200
  }

  it should "zip/map/collect" in {
    val SqLength = Field.zip(XField, YField).map(xy => xy._1*xy._1 + xy._2*xy._2)

    val AllSqLengths = Field.collect(SqLength)

    points(AllSqLengths).get shouldBe Seq[Long](12200, 14800, 17800, 21200)
  }

  it should "nested field with zip/map/collect" in {
    val data = ObjectV("data" -> points)

    val SqLength = Field.zip(XField, YField).map(xy => xy._1*xy._1 + xy._2*xy._2)

    val AllSqLengths = DataField.collect(SqLength)

    data(AllSqLengths).get shouldBe Seq[Long](12200, 14800, 17800, 21200)
  }
}
