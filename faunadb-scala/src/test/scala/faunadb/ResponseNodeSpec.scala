package faunadb

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import faunadb.values.LazyValue
import org.scalatest.{Matchers, FlatSpec}

class ResponseNodeSpec extends FlatSpec with Matchers {
  val json = new ObjectMapper().registerModule(new DefaultScalaModule)

  "Response Node coercion" should "return None for invalid types" in {
    val tree = json.createObjectNode().put("some", "string")
    val node = new LazyValue(tree, json)
    node("some").asString shouldBe "string"

    node("some").asBooleanOpt shouldBe None
    node("some").asNumberOpt shouldBe None
    node("some").asDoubleOpt shouldBe None
    node("some").asRefOpt shouldBe None

    node("some").asArrayOpt shouldBe None
    node("some").asObjectOpt shouldBe None
    node("some").asKeyOpt shouldBe None
    node("some").asClassOpt shouldBe None
    node("some").asDatabaseOpt shouldBe None
    node("some").asEventOpt shouldBe None
    node("some").asIndexOpt shouldBe None
    node("some").asPageOpt shouldBe None
    node("some").asSetOpt shouldBe None

    val tree2 = json.createObjectNode().set("some", json.createArrayNode().add("array"))
    val node2 = new LazyValue(tree2, json)
    node2("some").asObjectOpt shouldBe None
    node2("some").asStringOpt shouldBe None

    val tree3 = json.createObjectNode().set("some", json.createObjectNode().put("object", "key"))
    val node3 = new LazyValue(tree3, json)
    node3("some").asArrayOpt shouldBe None
    node3("some").asStringOpt shouldBe None
  }

}
