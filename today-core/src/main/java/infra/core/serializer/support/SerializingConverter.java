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

package infra.core.serializer.support;

import infra.core.conversion.Converter;
import infra.core.serializer.DefaultSerializer;
import infra.core.serializer.Serializer;
import infra.lang.Assert;

/**
 * A {@link Converter} that delegates to a
 * {@link Serializer}
 * to convert an object to a byte array.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @since 4.0
 */
public class SerializingConverter implements Converter<Object, byte[]> {

  private final Serializer<Object> serializer;

  /**
   * Create a default {@code SerializingConverter} that uses standard Java serialization.
   */
  public SerializingConverter() {
    this.serializer = new DefaultSerializer();
  }

  /**
   * Create a {@code SerializingConverter} that delegates to the provided {@link Serializer}.
   */
  public SerializingConverter(Serializer<Object> serializer) {
    Assert.notNull(serializer, "Serializer is required");
    this.serializer = serializer;
  }

  /**
   * Serializes the source object and returns the byte array result.
   */
  @Override
  public byte[] convert(Object source) {
    try {
      return this.serializer.serializeToByteArray(source);
    }
    catch (Throwable ex) {
      throw new SerializationFailedException("Failed to serialize object using " +
              this.serializer.getClass().getSimpleName(), ex);
    }
  }

}
