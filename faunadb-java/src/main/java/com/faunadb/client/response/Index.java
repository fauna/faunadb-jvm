package com.faunadb.client.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.faunadb.client.types.LazyValue;
import com.faunadb.client.types.Ref;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.lang.String.join;

/**
 * A FaunaDB Index response. This, like other response types, is created by coercing a {@link com.faunadb.client.types.Value}
 * using one of the conversion methods.
 *
 * <p><i>Reference:</i> <a href="https://faunadb.com/documentation#objects-indexes">FaunaDB Index Object</a></p>
 *
 * @see LazyValue#asIndex()
 */
public final class Index extends Instance {

  @JsonProperty("unique")
  private final Boolean unique;
  @JsonProperty("active")
  private final Boolean active;
  @JsonProperty("name")
  private final String name;
  @JsonProperty("source")
  private final Ref source;
  @JsonProperty("path")
  private final String path;
  @JsonProperty("terms")
  private final ImmutableList<ImmutableMap<String, String>> terms;

  @JsonCreator
  Index(@JsonProperty("ref") Ref ref,
        @JsonProperty("class") Ref classRef,
        @JsonProperty("ts") Long ts,
        @JsonProperty("unique") Boolean unique,
        @JsonProperty("active") Boolean active,
        @JsonProperty("name") String name,
        @JsonProperty("source") Ref source,
        @JsonProperty("path") String path,
        @JsonProperty("terms") @JsonDeserialize(using = TermsDeserializer.class)
        ImmutableList<ImmutableMap<String, String>> terms,
        @JsonProperty("data") ImmutableMap<String, LazyValue> data) {
    super(ref, classRef, ts, data);
    this.unique = unique;
    this.active = active;
    this.name = name;
    this.source = source;
    this.path = path;
    if (terms == null) {
      this.terms = ImmutableList.of();
    } else {
      this.terms = terms;
    }
  }

  /**
   * Returns whether the index is unique.
   */
  public Boolean unique() {
    return unique;
  }

  /**
   * Returns whether the index is currently active.
   */
  public Boolean active() {
    return active;
  }

  /**
   * Returns the name of the index.
   */
  public String name() {
    return name;
  }

  /**
   * Returns the ref of the source of this index's data.
   */
  public Ref source() {
    return source;
  }

  /**
   * Returns the path in instance data that is being indexed.
   */
  public String path() {
    return path;
  }

  /**
   * Returns the index's terms.
   */
  public ImmutableList<ImmutableMap<String, String>> terms() {
    return terms;
  }

  @Override
  public String toString() {
    return "Index(" + Joiner.on(", ").join(
      "ref: " + ref(),
      "class: " + classRef(),
      "ts: " + ts(),
      "unique: " + unique(),
      "active: " + active(),
      "name: " + name(),
      "source: " + source(),
      "path: " + path(),
      "terms: " + terms()
    ) + ")";
  }

  private static class TermsDeserializer extends JsonDeserializer<ImmutableList<ImmutableMap<String, String>>> {

    private static final TypeReference<List<HashMap<String, Object>>> LIST_OF_TERMS =
        new TypeReference<List<HashMap<String, Object>>>() {};

    @Override
    public ImmutableList<ImmutableMap<String, String>> deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException {

      List<HashMap<String, Object>> elements = p.readValueAs(LIST_OF_TERMS);
      if (elements == null) return null;

      Builder<ImmutableMap<String, String>> terms = ImmutableList.builder();

      for (HashMap<String, Object> element : elements) {
        for (Map.Entry<String, Object> entry : element.entrySet()) {
          terms.add(parseTerm(entry.getKey(), entry.getValue()));
        }
      }

      return terms.build();
    }

    @SuppressWarnings("unchecked")
    private ImmutableMap<String, String> parseTerm(String key, Object value) {
      if ("path".equals(key)) return ImmutableMap.of("path", (String) value);
      if ("field".equals(key)) return ImmutableMap.of("path", join(".", (List<String>) value));

      throw new IllegalArgumentException(format("Can not deserialize index term \"%s\" with value \"%s\"", key, value));
    }
  }

}
