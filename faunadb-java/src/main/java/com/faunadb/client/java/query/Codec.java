package com.faunadb.client.java.query;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import com.faunadb.client.java.query.Cursor.*;

public class Codec {
  public static class PaginateSerializer extends JsonSerializer<Paginate> {
    @Override
    public void serialize(Paginate paginate, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
      jsonGenerator.writeStartObject();
      jsonGenerator.writeObjectField("paginate", paginate.resource());

      if (paginate.ts().isPresent()) {
        jsonGenerator.writeNumberField("ts", paginate.ts().get());
      }

      if (paginate.cursor().isPresent()) {
        Cursor cursor = paginate.cursor().get();
        if (cursor instanceof Before) {
          jsonGenerator.writeObjectField("before", ((Before)cursor).ref());
        } else if (cursor instanceof After) {
          jsonGenerator.writeObjectField("after", ((After)cursor).ref());
        }

      }

      if (paginate.size().isPresent()) {
        jsonGenerator.writeNumberField("size", paginate.size().get());
      }

      jsonGenerator.writeEndObject();
    }
  }
}
