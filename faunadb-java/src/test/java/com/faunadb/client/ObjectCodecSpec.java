package com.faunadb.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.client.types.Codec;
import com.faunadb.client.types.Field;
import com.faunadb.client.types.Result;
import com.faunadb.client.types.Value;
import com.faunadb.client.types.Value.NullV;
import com.faunadb.client.types.Value.ObjectV;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.faunadb.client.types.Codec.OBJECT;
import static com.faunadb.client.types.Codec.STRING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.scala.DefaultScalaModule;

public class ObjectCodecSpec {

  private final Codec<CustomObject> CUSTOM_OBJECT = new Codec<CustomObject>() {
    final Field<String> name = Field.at("data", "name").to(STRING);
    final Field<Long> age = Field.at("data", "age").to(LONG);

    @Override
    public Result<CustomObject> decode(Value value) {
      CustomObject res = new CustomObject();
      res.name = value.get(name);
      res.age = value.get(age).intValue();
      return Result.success(res);
    }

    @Override
    public Result<Value> encode(CustomObject value) {
      return Result.fail("not implemented");
    }
  };

  class CustomObject {
    String name;
    Integer age;
  }

  class ValueObject {
    @JsonProperty String something;
    @JsonProperty String another;
  }

  class NestedValueObject {
    @JsonProperty Map<String, String> aMap;
    @JsonProperty ValueObject nested;
  }

  private ObjectMapper json;

  @Before
  public void setUp() throws Exception {
    json = new ObjectMapper()
      .registerModule(new Jdk8Module())
      .registerModule(DefaultScalaModule);
  }

  @Test
  public void shouldConvertASingleMap() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("test", "value");
      
    ObjectV obj = json.convertValue(map, ObjectV.class);
    assertThat(obj.at("test").to(STRING).get(), equalTo("value"));

    JsonNode node = this.json.valueToTree(obj);
    assertThat(node.get("object").get("test").asText(), equalTo("value"));
  }

  @Test
  public void shouldConvertObjectWithNull() {
    ValueObject value = new ValueObject();
    value.something = "huh";
    value.another = null;

    ObjectV obj = json.convertValue(value, ObjectV.class);
    assertThat(obj.at("something").to(STRING).get(), equalTo("huh"));
    assertThat(obj.at("another"), CoreMatchers.<Value>is(NullV.NULL));

    JsonNode node = json.valueToTree(obj);
    assertThat(node.get("object").get("something").asText(), equalTo("huh"));
    assertThat(node.get("object").has("another"), is(true));
    assertThat(node.get("object").get("another").isNull(), is(true));
  }

  @Test
  public void shouldConvertNestedObject() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("some", "value");
      
    NestedValueObject nested = new NestedValueObject();
    nested.aMap = map;
    nested.nested = new ValueObject();
    nested.nested.something = "huh";
    nested.nested.another = null;

    ObjectV obj = json.convertValue(nested, ObjectV.class);
    assertThat(obj.at("aMap").to(OBJECT).get().get("some").to(STRING).get(), equalTo("value"));
    assertThat(obj.at("nested").to(OBJECT).get().get("something").to(STRING).get(), equalTo("huh"));
    assertThat(obj.at("nested").to(OBJECT).get().get("another"), CoreMatchers.<Value>is(NullV.NULL));

    JsonNode node = json.valueToTree(obj);
    assertThat(node.get("object").get("aMap").isObject(), is(true));
    assertThat(node.get("object").get("aMap").get("object").get("some").asText(), equalTo("value"));
    assertThat(node.get("object").get("nested").get("object").get("something").asText(), equalTo("huh"));
    assertThat(node.get("object").get("nested").get("object").has("another"), is(true));
    assertThat(node.get("object").get("nested").get("object").get("another").isNull(), is(true));
  }

  @Test
  public void shouldBeAbleToUseCustomCodec() throws Exception {
    Map<String, Value> kvs = new LinkedHashMap<>();
    Map<String, Value> data = new LinkedHashMap<>();

    data.put("name", new Value.StringV("John"));
    data.put("age", new Value.LongV(42));

    kvs.put("data", new ObjectV(data));

    ObjectV obj = new ObjectV(kvs);

    CustomObject custom = obj.to(CUSTOM_OBJECT).get();
    assertThat(custom.name, equalTo("John"));
    assertThat(custom.age, equalTo(42));
  }
}
