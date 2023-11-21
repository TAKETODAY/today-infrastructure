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

package cn.taketoday.core.serializer.support;

import java.io.ByteArrayInputStream;

import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.serializer.DefaultDeserializer;
import cn.taketoday.core.serializer.Deserializer;
import cn.taketoday.lang.Assert;

/**
 * A {@link Converter} that delegates to a
 * {@link Deserializer}
 * to convert data in a byte array to an object.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DeserializingConverter implements Converter<byte[], Object> {

  private final Deserializer<Object> deserializer;

  /**
   * Create a {@code DeserializingConverter} with default {@link java.io.ObjectInputStream}
   * configuration, using the "latest user-defined ClassLoader".
   *
   * @see DefaultDeserializer#DefaultDeserializer()
   */
  public DeserializingConverter() {
    this.deserializer = new DefaultDeserializer();
  }

  /**
   * Create a {@code DeserializingConverter} for using an {@link java.io.ObjectInputStream}
   * with the given {@code ClassLoader}.
   *
   * @see DefaultDeserializer#DefaultDeserializer(ClassLoader)
   */
  public DeserializingConverter(ClassLoader classLoader) {
    this.deserializer = new DefaultDeserializer(classLoader);
  }

  /**
   * Create a {@code DeserializingConverter} that delegates to the provided {@link Deserializer}.
   */
  public DeserializingConverter(Deserializer<Object> deserializer) {
    Assert.notNull(deserializer, "Deserializer is required");
    this.deserializer = deserializer;
  }

  @Override
  public Object convert(byte[] source) {
    ByteArrayInputStream byteStream = new ByteArrayInputStream(source);
    try {
      return this.deserializer.deserialize(byteStream);
    }
    catch (Throwable ex) {
      throw new SerializationFailedException("Failed to deserialize payload. " +
              "Is the byte array a result of corresponding serialization for " +
              this.deserializer.getClass().getSimpleName() + "?", ex);
    }
  }

}
