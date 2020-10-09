package faunadb

import faunadb.values._
import java.time.{ LocalDate, Instant }
import org.scalatest.{ FlatSpec, Matchers }

class CodecSpec extends FlatSpec with Matchers {

  // Decoding

  it should "decode to primitive types" in {
    LongV(10).to[Long].get shouldBe 10
    LongV(10).to[Int].get shouldBe 10
    LongV(10).to[Short].get shouldBe 10
    LongV(10).to[Byte].get shouldBe 10
    LongV(10).to[Char].get shouldBe 10

    DoubleV(3.14).to[Double].get shouldBe 3.14
    DoubleV(3.14).to[Float].get shouldBe 3.14f

    BooleanV(true).to[Boolean].get shouldBe true

    DateV("1970-01-03").to[LocalDate].get shouldBe LocalDate.parse("1970-01-03")
    TimeV("1970-01-01T00:05:00.000000000Z").to[Instant].get shouldBe Instant.parse("1970-01-01T00:05:00.000000000Z")

    BytesV(1, 2, 3, 4).to[Array[Byte]].get shouldBe Array[Byte](1, 2, 3, 4)
  }

  it should "decode nullable types" in {
    NullV.to[Option[LocalDate]].get shouldBe None
    NullV.to[Option[Instant]].get shouldBe None
    NullV.to[Option[String]].get shouldBe None
    NullV.to[Option[Array[Byte]]].get shouldBe None
    NullV.to[Option[Array[Long]]].get shouldBe None
    NullV.to[Option[Seq[Long]]].get shouldBe None
  }

  case class Product(description: String, price: Double)

  case class Order(customer: String, products: Seq[Product])

  implicit val productCodec: Codec[Product] = Codec.Record[Product]
  implicit val orderCodec: Codec[Order] = Codec.Record[Order]

  it should "decode to object" in {
    val product = ObjectV("description" -> StringV("laptop"), "price" -> DoubleV(999))

    product.to[Product].get shouldBe Product("laptop", 999)
  }

  it should "ignore the order of fields in ObjectV when decoding" in {
    val product = ObjectV("price" -> DoubleV(999), "description" -> StringV("laptop"), "ignoredField" -> StringV("ignored value"))

    product.to[Product].get shouldBe Product("laptop", 999)
  }

  it should "decode to nested objects" in {
    val product1 = ObjectV("description" -> StringV("laptop"), "price" -> DoubleV(999))
    val product2 = ObjectV("description" -> StringV("mouse"), "price" -> DoubleV(9.99))
    val order = ObjectV("customer" -> StringV("John"), "products" -> ArrayV(product1, product2))

    order.to[Order].get shouldBe Order("John", Seq(Product("laptop", 999), Product("mouse", 9.99)))
  }

  it should "decode to collections types" in {
    val array = ArrayV(StringV("value 0"), StringV("value 1"))

    array.to[Seq[String]].get shouldBe Seq("value 0", "value 1")
    array.to[List[String]].get shouldBe List("value 0", "value 1")
    array.to[Vector[String]].get shouldBe Vector("value 0", "value 1")
    array.to[IndexedSeq[String]].get shouldBe IndexedSeq("value 0", "value 1")
    array.to[Set[String]].get shouldBe Set("value 0", "value 1")
    array.to[Array[String]].get shouldBe Array("value 0", "value 1")
  }

  it should "decode to collections of objects" in {
    val product1 = ObjectV("description" -> StringV("laptop"), "price" -> DoubleV(999))
    val product2 = ObjectV("description" -> StringV("mouse"), "price" -> DoubleV(9.99))
    val products = ArrayV(product1, product2)

    products.to[Seq[Product]].get shouldBe Seq(Product("laptop", 999), Product("mouse", 9.99))
    products.to[List[Product]].get shouldBe List(Product("laptop", 999), Product("mouse", 9.99))
    products.to[Vector[Product]].get shouldBe Vector(Product("laptop", 999), Product("mouse", 9.99))
    products.to[IndexedSeq[Product]].get shouldBe IndexedSeq(Product("laptop", 999), Product("mouse", 9.99))
    products.to[Set[Product]].get shouldBe Set(Product("laptop", 999), Product("mouse", 9.99))
    products.to[Array[Product]].get shouldBe Array(Product("laptop", 999), Product("mouse", 9.99))
  }

  it should "decode to map type" in {
    val obj = ObjectV("key0" -> StringV("value0"), "key1" -> StringV("value1"))

    obj.to[Map[String, String]].get shouldBe Map("key0" -> "value0", "key1" -> "value1")
  }

  it should "decode to map of objects" in {
    val product1 = ObjectV("description" -> StringV("laptop"), "price" -> DoubleV(999))
    val product2 = ObjectV("description" -> StringV("mouse"), "price" -> DoubleV(9.99))
    val products = ObjectV("product1" -> product1, "product2" -> product2)

    products.to[Map[String, Product]].get shouldBe Map("product1" -> Product("laptop", 999), "product2" -> Product("mouse", 9.99))
  }

  it should "decode aliases" in {
    case class Wrapped(s: String)
    implicit val wrappedCodec = Codec.Alias[Wrapped, String](_.s, Wrapped)

    val v1 = StringV("hello")
    v1.to[Wrapped].get shouldBe Wrapped("hello")
    (Wrapped("hello"): Value) shouldBe v1

    case class DoubleWrapped(w: Wrapped)
    implicit val doubleWrappedCodec = Codec.Alias[DoubleWrapped, Wrapped](_.w, DoubleWrapped)

    val v2 = StringV("hello")
    v2.to[DoubleWrapped].get shouldBe DoubleWrapped(Wrapped("hello"))
    (DoubleWrapped(Wrapped("hello")): Value) shouldBe v2
  }

  case class GenericClass[T, U](a: T, b: U, c: NonGenericClass)

  case class NonGenericClass(x: Int)

  implicit val nonGenericClass: Codec[NonGenericClass] = Codec.Record[NonGenericClass]
  implicit val genericCodec1: Codec[GenericClass[Int, String]] = Codec.Record[GenericClass[Int, String]]
  implicit val genericCodec2: Codec[GenericClass[Long, Double]] = Codec.Record[GenericClass[Long, Double]]

  it should "decode using generic types" in {
    val obj1 = ObjectV("a" -> LongV(10), "b" -> StringV("str"), "c" -> ObjectV("x" -> LongV(10)))
    obj1.to[GenericClass[Int, String]].get shouldBe GenericClass(10, "str", NonGenericClass(10))

    val obj2 = ObjectV("a" -> LongV(10), "b" -> DoubleV(3.14), "c" -> ObjectV("x" -> LongV(10)))
    obj2.to[GenericClass[Long, Double]].get shouldBe GenericClass(10, 3.14, NonGenericClass(10))

    ArrayV(obj1).to[List[GenericClass[Int, String]]].get shouldBe List(GenericClass(10, "str", NonGenericClass(10)))
  }

  case class ClassWithOption(a: String, b: Option[Long])

  implicit val classWithOptionCodec: Codec[ClassWithOption] = Codec.Record[ClassWithOption]

  it should "decode class with Option" in {
    val obj1 = ObjectV("a" -> StringV("a"), "b" -> LongV(10))
    obj1.to[ClassWithOption].get shouldBe ClassWithOption("a", Some(10L))

    val obj2 = ObjectV("a" -> StringV("a"), "b" -> NullV)
    obj2.to[ClassWithOption].get shouldBe ClassWithOption("a", None)

    val obj3 = ObjectV("a" -> StringV("a"))
    obj3.to[ClassWithOption].get shouldBe ClassWithOption("a", None)
  }

  case class ClassWithEither(either: Either[String, Long])

  implicit val classWithEither: Codec[ClassWithEither] = Codec.Record[ClassWithEither]

  it should "decode class with Either" in {
    val obj1 = ObjectV("either" -> StringV("string value"))
    obj1.to[ClassWithEither].get shouldBe ClassWithEither(Left("string value"))

    val obj2 = ObjectV("either" -> LongV(10))
    obj2.to[ClassWithEither].get shouldBe ClassWithEither(Right(10))
  }

  sealed trait EnumTrait
  case class Variant1(i: Int) extends EnumTrait
  case class Variant2(s: String) extends EnumTrait
  case class Variant3(b: Boolean) extends EnumTrait
  case object Variant4 extends EnumTrait

  implicit val enumTrait = Codec.Union[EnumTrait]("tpe")(
    true -> Codec.Record[Variant1],
    2 -> Codec.Record[Variant2],
    "a" -> Codec.Record[Variant3],
    "4" -> Codec.Record(Variant4))

  it should "encode/decode enum trait" in {
    val obj1 = ObjectV("tpe" -> TrueV, "i" -> LongV(2))
    obj1.to[EnumTrait].get shouldBe Variant1(2)
    (Variant1(2): Value) shouldBe obj1

    val obj2 = ObjectV("tpe" -> LongV(2), "s" -> StringV("foo"))
    obj2.to[EnumTrait].get shouldBe Variant2("foo")
    (Variant2("foo"): Value) shouldBe obj2

    val obj3 = ObjectV("tpe" -> StringV("a"), "b" -> TrueV)
    obj3.to[EnumTrait].get shouldBe Variant3(true)
    (Variant3(true): Value) shouldBe obj3

    val obj4 = ObjectV("tpe" -> StringV("4"))
    obj4.to[EnumTrait].get shouldBe Variant4
    (Variant4: Value) shouldBe obj4

    val obj5 = ObjectV("tpe" -> StringV("x"), "x" -> FalseV)
    the [ValueReadException] thrownBy {
      obj5.to[EnumTrait].get
    } should have message """Error at /tpe: Expected Union tag: true, 2, "a", or "4"; found value "x" of type String."""
  }

  it should "decode tuples" in {
    ArrayV("string", 10L).to[(String, Long)].get shouldBe ("string", 10L)
    ArrayV("string", 10L, 20D).to[(String, Long, Double)].get shouldBe ("string", 10L, 20D)
    ArrayV("string", 10L, 20D, ArrayV("str0", "str1")).to[(String, Long, Double, Seq[String])].get shouldBe ("string", 10L, 20D, Seq("str0", "str1"))
    ArrayV("string", 10L, 20D, ArrayV("str0", "str1"), 0xff).to[(String, Long, Double, Seq[String], Int)].get shouldBe ("string", 10L, 20D, Seq("str0", "str1"), 0xff)

    ArrayV(1, 2, 3, 4, 5, 6).to[(Int, Int, Int, Int, Int, Int)].get shouldBe (1, 2, 3, 4, 5, 6)
    ArrayV(1, 2, 3, 4, 5, 6, 7).to[(Int, Int, Int, Int, Int, Int, Int)].get shouldBe (1, 2, 3, 4, 5, 6, 7)
    ArrayV(1, 2, 3, 4, 5, 6, 7, 8).to[(Int, Int, Int, Int, Int, Int, Int, Int)].get shouldBe (1, 2, 3, 4, 5, 6, 7, 8)
    ArrayV(1, 2, 3, 4, 5, 6, 7, 8, 9).to[(Int, Int, Int, Int, Int, Int, Int, Int, Int)].get shouldBe (1, 2, 3, 4, 5, 6, 7, 8, 9)
    ArrayV(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).to[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)].get shouldBe (1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

    implicit val tuple11 = Codec.Record[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)]
    ArrayV(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11).to[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)].get shouldBe (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

    ArrayV("string", 10L, "not used element").to[(String, Long)].get shouldBe ("string", 10L)
  }

  // Encoding

  it should "encode from primitive types" in {
    (10.toLong: Value) shouldBe LongV(10)
    (10.toInt: Value) shouldBe LongV(10)
    (10.toShort: Value) shouldBe LongV(10)
    (10.toByte: Value) shouldBe LongV(10)
    (10.toChar: Value) shouldBe LongV(10)

    (3.14: Value) shouldBe DoubleV(3.14)
    (3.14f: Value) shouldBe DoubleV(3.14f)

    (true: Value) shouldBe TrueV
    (false: Value) shouldBe FalseV

    (LocalDate.parse("1970-01-03"): Value) shouldBe DateV("1970-01-03")
    (Instant.parse("1970-01-01T00:05:00.000000000Z"): Value) shouldBe TimeV("1970-01-01T00:05:00.000000000Z")

    (Array[Byte](1, 2, 3, 4): Value) shouldBe BytesV(1, 2, 3, 4)
  }

  it should "encode nullable types" in {
    (None: Value) shouldBe NullV

    ((null: String): Value) shouldBe NullV
    ((null: LocalDate): Value) shouldBe NullV
    ((null: Instant): Value) shouldBe NullV
    ((null: Array[Byte]): Value) shouldBe NullV
    ((null: Array[Long]): Value) shouldBe NullV
    ((null: Seq[Long]): Value) shouldBe NullV

    (Product(null, 10): Value) shouldBe ObjectV("description" -> NullV, "price" -> DoubleV(10))
  }

  it should "encode an object" in {
    val product: Value = Product("laptop", 999)

    product shouldBe ObjectV("description" -> StringV("laptop"), "price" -> DoubleV(999))
  }

  it should "ignore the order of fields in ObjectV when encoding" in {
    val product: Value = Product("laptop", 999)

    product shouldBe ObjectV("price" -> DoubleV(999), "description" -> StringV("laptop"))
  }

  it should "encode nested objects" in {
    val order: Value = Order("John", Seq(Product("laptop", 999), Product("mouse", 9.99)))

    order shouldBe ObjectV(
      "customer" -> StringV("John"),
      "products" -> ArrayV(
        ObjectV("description" -> StringV("laptop"), "price" -> DoubleV(999)),
        ObjectV("description" -> StringV("mouse"), "price" -> DoubleV(9.99)))
    )
  }

  it should "encode collections types" in {
    val array = ArrayV(StringV("value 0"), StringV("value 1"))

    (Seq("value 0", "value 1"): Value) shouldBe array
    (List("value 0", "value 1"): Value) shouldBe array
    (Vector("value 0", "value 1"): Value) shouldBe array
    (IndexedSeq("value 0", "value 1"): Value) shouldBe array
    (Set("value 0", "value 1"): Value) shouldBe array
    (Array[String]("value 0", "value 1"): Value) shouldBe array
  }

  it should "encode collections of objects" in {
    val product1 = ObjectV("description" -> StringV("laptop"), "price" -> DoubleV(999))
    val product2 = ObjectV("description" -> StringV("mouse"), "price" -> DoubleV(9.99))
    val products = ArrayV(product1, product2)

    (Seq(Product("laptop", 999), Product("mouse", 9.99)): Value) shouldBe products
    (List(Product("laptop", 999), Product("mouse", 9.99)): Value) shouldBe products
    (Vector(Product("laptop", 999), Product("mouse", 9.99)): Value) shouldBe products
    (IndexedSeq(Product("laptop", 999), Product("mouse", 9.99)): Value) shouldBe products
    (Set[Product](Product("laptop", 999), Product("mouse", 9.99)): Value) shouldBe products
    (Array[Product](Product("laptop", 999), Product("mouse", 9.99)): Value) shouldBe products
  }

  it should "encode to map type" in {
    val obj = ObjectV("key0" -> StringV("value0"), "key1" -> StringV("value1"))

    (Map("key0" -> "value0", "key1" -> "value1"): Value) shouldBe obj
  }

  it should "encode to map of objects" in {
    val product1 = ObjectV("description" -> StringV("laptop"), "price" -> DoubleV(999))
    val product2 = ObjectV("description" -> StringV("mouse"), "price" -> DoubleV(9.99))
    val products = ObjectV("product1" -> product1, "product2" -> product2)

    (Map("product1" -> Product("laptop", 999), "product2" -> Product("mouse", 9.99)): Value) shouldBe products
  }

  it should "encode using generic types" in {
    val obj1 = ObjectV("a" -> LongV(10), "b" -> StringV("str"), "c" -> ObjectV("x" -> LongV(10)))
    val obj2 = ObjectV("a" -> LongV(10), "b" -> DoubleV(3.14), "c" -> ObjectV("x" -> LongV(10)))

    (GenericClass[Int, String](10, "str", NonGenericClass(10)): Value) shouldBe obj1
    (GenericClass[Long, Double](10, 3.14, NonGenericClass(10)): Value) shouldBe obj2
    (List(GenericClass[Int, String](10, "str", NonGenericClass(10))): Value) shouldBe ArrayV(obj1)
  }

  it should "encode an Option" in {
    (Some(1): Value) shouldBe LongV(1)
    (None: Value) shouldBe NullV
  }

  it should "encode class with Option" in {
    (ClassWithOption("a", Some(10L)): Value) shouldBe ObjectV("a" -> StringV("a"), "b" -> LongV(10))

    (ClassWithOption("a", None): Value) shouldBe ObjectV("a" -> StringV("a"), "b" -> NullV)
  }

  it should "encode an Either" in {
    (Left(1): Value) shouldBe LongV(1)
    (Right(2): Value) shouldBe LongV(2)
  }

  it should "encode class with Either" in {
    (ClassWithEither(Left("string value")): Value) shouldBe ObjectV("either" -> StringV("string value"))

    (ClassWithEither(Right(10)): Value) shouldBe ObjectV("either" -> LongV(10))
  }

  it should "encode tuples" in {
    (("string", 10L): Value) shouldBe ArrayV("string", 10L)
    (("string", 10L, 20D): Value) shouldBe ArrayV("string", 10L, 20D)
    (("string", 10L, 20D, Seq("str0", "str1")): Value) shouldBe ArrayV("string", 10L, 20D, ArrayV("str0", "str1"))
    (("string", 10L, 20D, Seq("str0", "str1"), 0xff): Value) shouldBe ArrayV("string", 10L, 20D, ArrayV("str0", "str1"), 0xff)

    ((1, 2, 3, 4, 5, 6): Value) shouldBe ArrayV(1, 2, 3, 4, 5, 6)
    ((1, 2, 3, 4, 5, 6, 7): Value) shouldBe ArrayV(1, 2, 3, 4, 5, 6, 7)
    ((1, 2, 3, 4, 5, 6, 7, 8): Value) shouldBe ArrayV(1, 2, 3, 4, 5, 6, 7, 8)
    ((1, 2, 3, 4, 5, 6, 7, 8, 9): Value) shouldBe ArrayV(1, 2, 3, 4, 5, 6, 7, 8, 9)
    ((1, 2, 3, 4, 5, 6, 7, 8, 9, 10): Value) shouldBe ArrayV(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

    implicit val tuple11 = Codec.Record[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)]
    ((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11): Value) shouldBe ArrayV(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
  }

  it should "works with for-comprehensions" in {
    {
      val obj = ObjectV("x" -> LongV(10), "y" -> LongV(20))

      val xy = for {
        x <- obj("x").to[Long]
        y <- obj("y").to[Long]
      } yield {
        (x, y)
      }

      xy.isSuccess shouldBe true
      xy.get shouldBe (10L, 20L)
    }

    {
      val obj = ObjectV("y" -> LongV(20))

      val xy = for {
        x <- obj("x").to[Long]
        y <- obj("y").to[Long]
      } yield {
        (x, y)
      }

      xy.isFailure shouldBe true
      the [ValueReadException] thrownBy {
        xy.get
      } should have message "Error at /x: Value not found"
    }

    {
      val obj = ObjectV("x" -> LongV(10))

      val xy = for {
        x <- obj("x").to[Long]
        y <- obj("y").to[Option[Long]]
      } yield {
        (x, y)
      }

      xy.isSuccess shouldBe true
      xy.get shouldBe (10L, Option.empty[Long])
    }

    {
      val obj = ObjectV("x" -> LongV(10), "y" -> ObjectV())

      val xy = for {
        x <- obj("x").to[Long]
        y <- obj("y", "z").to[Option[Long]]
      } yield {
        (x, y)
      }

      xy.isSuccess shouldBe true
      xy.get shouldBe (10L, Option.empty[Long])
    }
  }

  // Errors

  it should "test for runtime errors" in {
    the [ValueReadException] thrownBy {
      StringV("10").to[List[Long]].get
    } should have message "Error at /: Expected Array; found value \"10\" of type String."

    the [ValueReadException] thrownBy {
      ArrayV(StringV("10")).to[List[Long]].get
    } should have message "Error at /0: Expected Long; found value \"10\" of type String."

    the [ValueReadException] thrownBy {
      ObjectV("either" -> DoubleV(10)).to[ClassWithEither].get
    } should have message "Error at /either: Expected Either String or long; found value 10.0 of type Double."

    the [ValueReadException] thrownBy {
      ObjectV("x" -> LongV(10), "y" -> LongV(20)).to[Map[String, String]].get
    } should have message "Error at /x: Expected String; found value 10 of type Long., Error at /y: Expected String; found value 20 of type Long."

    the [ValueReadException] thrownBy {
      ObjectV("a" -> ObjectV("x" -> LongV(1), "y" -> LongV(2))).to[Map[String, Map[String, String]]].get
    } should have message "Error at /a/x: Expected String; found value 1 of type Long., Error at /a/y: Expected String; found value 2 of type Long."
  }

}
