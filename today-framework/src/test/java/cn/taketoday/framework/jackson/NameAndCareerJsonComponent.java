/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.jackson;

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
 * @author Paul Aly
 */
@JsonComponent(type = NameAndCareer.class)
public class NameAndCareerJsonComponent {

  static class Serializer extends JsonObjectSerializer<Name> {

    @Override
    protected void serializeObject(Name value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      jgen.writeStringField("name", value.getName());
    }

  }

  static class Deserializer extends JsonObjectDeserializer<Name> {

    @Override
    protected Name deserializeObject(JsonParser jsonParser, DeserializationContext context, ObjectCodec codec,
            JsonNode tree) throws IOException {
      String name = nullSafeValue(tree.get("name"), String.class);
      String career = nullSafeValue(tree.get("career"), String.class);
      return new NameAndCareer(name, career);
    }

  }

}
