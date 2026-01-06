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

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Helper base class for {@link ValueSerializer} implementations that serialize objects.
 *
 * @param <T> the supported object type
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ObjectValueDeserializer
 * @since 4.0
 */
public abstract class ObjectValueSerializer<T> extends ValueSerializer<T> {

  @Override
  public final void serialize(T value, JsonGenerator jgen, SerializationContext context) {
    jgen.writeStartObject();
    serializeObject(value, jgen, context);
    jgen.writeEndObject();
  }

  /**
   * Serialize JSON content into the value type this serializer handles.
   *
   * @param value the source value
   * @param jgen the JSON generator
   * @param context the serialization context
   */
  protected abstract void serializeObject(T value, JsonGenerator jgen, SerializationContext context);

}
