/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.app.jackson;

import infra.app.jackson.types.NameAndAge;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample {@link JacksonComponent @JacksonComponent} used for tests.
 *
 * @author Phillip Webb
 */
@JacksonComponent
public class NameAndAgeJacksonComponent {

  static class Serializer extends ObjectValueSerializer<NameAndAge> {

    @Override
    protected void serializeObject(NameAndAge value, JsonGenerator jgen, SerializationContext context) {
      jgen.writeStringProperty("theName", value.getName());
      jgen.writeNumberProperty("theAge", value.getAge());
    }

  }

  static class Deserializer extends ObjectValueDeserializer<NameAndAge> {

    @Override
    protected NameAndAge deserializeObject(JsonParser jsonParser, DeserializationContext context, JsonNode tree) {
      String name = nullSafeValue(tree.get("name"), String.class);
      Integer age = nullSafeValue(tree.get("age"), Integer.class);
      assertThat(age).isNotNull();
      return NameAndAge.create(name, age);
    }

  }

}
