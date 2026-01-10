/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core.serializer.support;

import java.io.ByteArrayInputStream;

import infra.core.conversion.Converter;
import infra.core.serializer.DefaultDeserializer;
import infra.core.serializer.Deserializer;
import infra.lang.Assert;

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
