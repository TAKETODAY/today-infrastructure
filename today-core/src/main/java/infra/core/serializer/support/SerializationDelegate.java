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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import infra.core.serializer.DefaultDeserializer;
import infra.core.serializer.DefaultSerializer;
import infra.core.serializer.Deserializer;
import infra.core.serializer.Serializer;
import infra.lang.Assert;

/**
 * A convenient delegate with pre-arranged configuration state for common
 * serialization needs. Implements {@link Serializer} and {@link Deserializer}
 * itself, so can also be passed into such more specific callback methods.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SerializationDelegate implements Serializer<Object>, Deserializer<Object> {

  private final Serializer<Object> serializer;

  private final Deserializer<Object> deserializer;

  /**
   * Create a {@code SerializationDelegate} with a default serializer/deserializer
   * for the given {@code ClassLoader}.
   *
   * @see DefaultDeserializer
   * @see DefaultDeserializer#DefaultDeserializer(ClassLoader)
   */
  public SerializationDelegate(ClassLoader classLoader) {
    this.serializer = new DefaultSerializer();
    this.deserializer = new DefaultDeserializer(classLoader);
  }

  /**
   * Create a {@code SerializationDelegate} with the given serializer/deserializer.
   *
   * @param serializer the {@link Serializer} to use (never {@code null)}
   * @param deserializer the {@link Deserializer} to use (never {@code null)}
   */
  public SerializationDelegate(Serializer<Object> serializer, Deserializer<Object> deserializer) {
    Assert.notNull(serializer, "Serializer is required");
    Assert.notNull(deserializer, "Deserializer is required");
    this.serializer = serializer;
    this.deserializer = deserializer;
  }

  @Override
  public void serialize(Object object, OutputStream outputStream) throws IOException {
    this.serializer.serialize(object, outputStream);
  }

  @Override
  public Object deserialize(InputStream inputStream) throws IOException {
    return this.deserializer.deserialize(inputStream);
  }

}
