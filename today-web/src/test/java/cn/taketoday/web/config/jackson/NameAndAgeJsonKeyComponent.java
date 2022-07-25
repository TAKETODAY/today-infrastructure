/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.config.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Sample {@link JsonComponent @JsonComponent} used for tests.
 *
 * @author Paul Aly
 */
@JsonComponent(type = NameAndAge.class, scope = JsonComponent.Scope.KEYS)
public class NameAndAgeJsonKeyComponent {

  static class Serializer extends JsonSerializer<NameAndAge> {

    @Override
    public void serialize(NameAndAge value, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
      jgen.writeFieldName(value.asKey());
    }

  }

  static class Deserializer extends KeyDeserializer {

    @Override
    public NameAndAge deserializeKey(String key, DeserializationContext ctxt) throws IOException {
      String[] keys = key.split("is");
      return new NameAndAge(keys[0].trim(), Integer.parseInt(keys[1].trim()));
    }

  }

}
