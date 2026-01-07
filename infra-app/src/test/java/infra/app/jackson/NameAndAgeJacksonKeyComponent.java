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
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Sample {@link JacksonComponent @JacksonComponent} used for tests.
 *
 * @author Paul Aly
 */
@JacksonComponent(type = NameAndAge.class, scope = JacksonComponent.Scope.KEYS)
public class NameAndAgeJacksonKeyComponent {

  static class Serializer extends ValueSerializer<NameAndAge> {

    @Override
    public void serialize(NameAndAge value, JsonGenerator jgen, SerializationContext serializers) {
      jgen.writeName(value.asKey());
    }

  }

  static class Deserializer extends KeyDeserializer {

    @Override
    public NameAndAge deserializeKey(String key, DeserializationContext ctxt) {
      String[] keys = key.split("is");
      return NameAndAge.create(keys[0].trim(), Integer.parseInt(keys[1].trim()));
    }

  }

}
