/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Sample {@link JsonComponent @JsonComponent} used for tests.
 *
 * @author Phillip Webb
 */
@JsonComponent
public class NameAndAgeJsonComponent {

  static class Serializer extends JsonObjectSerializer<NameAndAge> {

    @Override
    protected void serializeObject(NameAndAge value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {
      jgen.writeStringField("name", value.getName());
      jgen.writeNumberField("age", value.getAge());
    }

  }

  static class Deserializer extends JsonObjectDeserializer<NameAndAge> {

    @Override
    protected NameAndAge deserializeObject(JsonParser jsonParser, DeserializationContext context, ObjectCodec codec,
            JsonNode tree) throws IOException {
      String name = nullSafeValue(tree.get("name"), String.class);
      Integer age = nullSafeValue(tree.get("age"), Integer.class);
      return new NameAndAge(name, age);
    }

  }

}
