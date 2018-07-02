package com.faunadb.client;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.List;

public class HttpResponses {
  static class Codec {
    static class ValidationFailureDeserializer extends JsonDeserializer<ValidationFailure> {
      @Override
      public ValidationFailure deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectMapper json = (ObjectMapper) jsonParser.getCodec();
        TypeFactory tf = deserializationContext.getTypeFactory();
        JsonNode tree = json.readTree(jsonParser);

        if (!tree.has("field")) {
          throw new JsonParseException("Cannot deserialize ValidationFailure: no 'field' field.", jsonParser.getTokenLocation());
        }

        ImmutableList<String> field = json.convertValue(tree.get("field"), tf.constructCollectionType(ImmutableList.class, String.class));

        if (!tree.has("code")) {
          throw new JsonParseException("Cannot deserialize ValidationFailure: no 'code' field.", jsonParser.getTokenLocation());
        }

        String code = tree.get("code").asText();

        String description;
        if (tree.has("description")) {
          description = tree.get("description").asText();
        } else if (tree.has("reason")) {
          description = tree.get("reason").asText();
        } else {
          throw new JsonParseException("Cannot deserialize ValidationFailure: no 'description' field.", jsonParser.getTokenLocation());
        }

        return new ValidationFailure(field, code, description);
      }
    }

    static class QueryErrorDeserializer extends JsonDeserializer<QueryError> {
      @Override
      public QueryError deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
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
        } else {
          throw new JsonParseException("Cannot deserialize QueryError: no 'description' field.", jsonParser.getTokenLocation());
        }

        ImmutableList<ValidationFailure> failures;
        if (tree.has("failures")) {
          failures = json.convertValue(tree.get("failures"), tf.constructCollectionType(ImmutableList.class, ValidationFailure.class));
        } else {
          failures = ImmutableList.of();
        }

        return new QueryError(position, code, description, failures);
      }
    }
  }

  @JsonDeserialize(using = Codec.ValidationFailureDeserializer.class)
  public static class ValidationFailure {
    private final ImmutableList<String> field;
    private final String code;
    private final String description;

    public ValidationFailure(
      ImmutableList<String> field,
      String code,
      String description) {
      this.field = field;
      this.code = code;
      this.description = description;
    }

    public String code() {
      return code;
    }

    public String description() {
      return description;
    }

    public List<String> field() {
      return field;
    }
  }

  @JsonDeserialize(using = Codec.QueryErrorDeserializer.class)
  public static class QueryError {
    private final ImmutableList<String> position;
    private final String code;
    private final String description;
    private final ImmutableList<ValidationFailure> failures;

    public QueryError(List<String> position,
                      String code,
                      String description,
                      ImmutableList<ValidationFailure> failures) {
      this.position = ImmutableList.copyOf(position);
      this.code = code;
      this.description = description;
      this.failures = failures;
    }

    public List<String> position() {
      return position;
    }

    public String code() {
      return code;
    }

    public String description() {
      return description;
    }

    public List<ValidationFailure> failures() {
      return failures;
    }
  }

  public static class QueryErrorResponse {
    public static QueryErrorResponse create(int status, List<QueryError> errors) {
      return new QueryErrorResponse(status, ImmutableList.copyOf(errors));
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

    public List<QueryError> errors() {
      return errors;
    }
  }
}
