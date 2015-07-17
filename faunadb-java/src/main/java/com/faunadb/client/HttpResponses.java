package com.faunadb.client;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;

public class HttpResponses {
  static class Codec {
    static class ParamErrorDeserializer extends JsonDeserializer<ParamError> {
      @Override
      public ParamError deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectMapper json = (ObjectMapper) jsonParser.getCodec();
        JsonNode tree = json.readTree(jsonParser);

        if (!tree.has("error")) {
          throw new JsonParseException("Cannot deserialize ParamError: no 'error' field.", jsonParser.getTokenLocation());
        }

        String error = tree.get("error").asText();

        String description;
        if (tree.has("description")) {
          description = tree.get("description").asText();
        } else if (tree.has("reason")) {
          description = tree.get("reason").asText();
        } else {
          throw new JsonParseException("Cannot deserialize ParamError: no 'description' field.", jsonParser.getTokenLocation());
        }

        return new ParamError(error, description);
      }
    }

    static class QueryErrorDeserializer extends JsonDeserializer<QueryError> {
      @Override
      public QueryError deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ObjectMapper json = (ObjectMapper) jsonParser.getCodec();
        JsonNode tree = json.readTree(jsonParser);

        TypeFactory tf = deserializationContext.getTypeFactory();

        ImmutableList<String> position;
        if (tree.has("position")) {
          position = json.convertValue(tree.get("position"), tf.constructCollectionType(ImmutableList.class, String.class));
        } else {
          position = ImmutableList.of();
        }

        if (!tree.has("code")) {
          throw new JsonParseException("Cannot deserialize QueryError: no 'code' field.", jsonParser.getTokenLocation());
        }

        String code = tree.get("code").asText();

        String description;
        if (tree.has("description")) {
          description = tree.get("description").asText();
        } else if (tree.has("reason")) {
          description = tree.get("reason").asText();
        } else {
          throw new JsonParseException("Cannot deserialize QueryError: no 'description' field.", jsonParser.getTokenLocation());
        }

        ImmutableMap<String, ParamError> parameters;
        if (tree.has("parameters")) {
          parameters = json.convertValue(tree.get("parameters"), tf.constructMapLikeType(ImmutableMap.class, String.class, ParamError.class));
        } else {
          parameters = ImmutableMap.of();
        }

        return new QueryError(position, code, description, parameters);
      }
    }
  }

  @JsonDeserialize(using = Codec.ParamErrorDeserializer.class)
  public static class ParamError {
    private final String error;
    private final String description;

    public ParamError(
      String error,
      String description) {
      this.error = error;
      this.description = description;
    }

    public String error() {
      return error;
    }

    public String description() {
      return description;
    }
  }

  @JsonDeserialize(using = Codec.QueryErrorDeserializer.class)
  public static class QueryError {
    private final ImmutableList<String> position;
    private final String code;
    private final String description;
    private final ImmutableMap<String, ParamError> parameters;

    public QueryError(ImmutableList<String> position,
                      String code,
                      String description,
                      ImmutableMap<String, ParamError> parameters) {
      this.position = position;
      this.code = code;
      this.description = description;
      this.parameters = parameters;
    }

    public ImmutableList<String> position() {
      return position;
    }

    public String code() {
      return code;
    }

    public String description() {
      return description;
    }

    public ImmutableMap<String, ParamError> parameters() {
      return parameters;
    }
  }

  public static class QueryErrorResponse {
    public static QueryErrorResponse create(int status, ImmutableList<QueryError> errors) {
      return new QueryErrorResponse(status, errors);
    }

    private final int status;
    private final ImmutableList<QueryError> errors;

    QueryErrorResponse(int status, ImmutableList<QueryError> errors) {
      this.status = status;
      this.errors = errors;
    }

    public int status() {
      return status;
    }

    public ImmutableList<QueryError> errors() {
      return errors;
    }
  }
}
