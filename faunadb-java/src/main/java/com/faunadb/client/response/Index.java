package com.faunadb.client.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.LazyValue;
import com.faunadb.client.types.Ref;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * An immutable value-type representation of a FaunaDB Index response. This, like other
 * response types, is created by coercing a {@link LazyValue} using one of the conversion methods.
 *
 * <p><i>Reference:</i> <a href="https://faunadb.com/documentation#guide-resource_types-indexes">FaunaDB Resource Types</a></p>
 *
 * @see LazyValue#asIndex()
 */
public final class Index {
  @JsonProperty("ref")
  private final Ref ref;
  @JsonProperty("class")
  private final Ref classRef;
  @JsonProperty("ts")
  private final Long ts;
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
        @JsonProperty("terms") ImmutableList<ImmutableMap<String, String>> terms) {
    this.ref = ref;
    this.classRef = classRef;
    this.ts = ts;
    this.unique = unique;
    this.active = active;
    this.name = name;
    this.source = source;
    this.path = path;
    this.terms = terms;
  }

  /**
   * Returns the ref of this Index
   */
  public Ref ref() {
    return ref;
  }

  /**
   * Returns the ref of the class of this resource. In this case, {@code indexes}.
   */
  public Ref classRef() {
    return classRef;
  }

  /**
   * Returns the timestamp of this Index.
   */
  public Long ts() {
    return ts;
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
}
