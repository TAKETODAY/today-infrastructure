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

import infra.app.jackson.types.Name;
import infra.app.jackson.types.NameAndCareer;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;

/**
 * Sample {@link JacksonComponent @JacksonComponent} used for tests.
 *
 * @author Paul Aly
 */
@JacksonComponent(type = NameAndCareer.class)
public class NameAndCareerJacksonComponent {

  static class Serializer extends ObjectValueSerializer<Name> {

    @Override
    protected void serializeObject(Name value, JsonGenerator jgen, SerializationContext context) {
      jgen.writeStringProperty("name", value.getName());
    }

  }

  static class Deserializer extends ObjectValueDeserializer<Name> {

    @Override
    protected Name deserializeObject(JsonParser jsonParser, DeserializationContext context, JsonNode tree) {
      String name = nullSafeValue(tree.get("name"), String.class);
      String career = nullSafeValue(tree.get("career"), String.class);
      return new NameAndCareer(name, career);
    }

  }

}
