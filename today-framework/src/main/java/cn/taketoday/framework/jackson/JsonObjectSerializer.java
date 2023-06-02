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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Helper base class for {@link JsonSerializer} implementations that serialize objects.
 *
 * @param <T> the supported object type
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JsonObjectDeserializer
 * @since 4.0
 */
public abstract class JsonObjectSerializer<T> extends JsonSerializer<T> {

  @Override
  public final void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
    try {
      jgen.writeStartObject();
      serializeObject(value, jgen, provider);
      jgen.writeEndObject();
    }
    catch (Exception ex) {
      if (ex instanceof IOException) {
        throw (IOException) ex;
      }
      throw new JsonMappingException(jgen, "Object serialize error", ex);
    }
  }

  /**
   * Serialize JSON content into the value type this serializer handles.
   *
   * @param value the source value
   * @param jgen the JSON generator
   * @param provider the serializer provider
   * @throws IOException on error
   */
  protected abstract void serializeObject(T value, JsonGenerator jgen, SerializerProvider provider)
          throws IOException;

}
