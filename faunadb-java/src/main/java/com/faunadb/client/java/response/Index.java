package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Ref;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class Index {
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

  public Index(Ref ref, Ref classRef, Long ts, Boolean unique, Boolean active, String name, Ref source, String path, ImmutableList<ImmutableMap<String, String>> terms) {
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

  public Ref ref() {
    return ref;
  }

  public Ref classRef() {
    return classRef;
  }

  public Long ts() {
    return ts;
  }

  public Boolean unique() {
    return unique;
  }

  public Boolean active() {
    return active;
  }

  public String name() {
    return name;
  }

  public Ref source() {
    return source;
  }

  public String path() {
    return path;
  }

  public ImmutableList<ImmutableMap<String, String>> terms() {
    return terms;
  }

}
