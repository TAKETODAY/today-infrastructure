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

package infra.core.serializer;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import infra.core.ConfigurableObjectInputStream;

/**
 * A default {@link Deserializer} implementation that reads an input stream
 * using Java serialization.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @see ObjectInputStream
 * @since 4.0
 */
public class DefaultDeserializer implements Deserializer<Object> {

  @Nullable
  private final ClassLoader classLoader;

  /**
   * Create a {@code DefaultDeserializer} with default {@link ObjectInputStream}
   * configuration, using the "latest user-defined ClassLoader".
   */
  public DefaultDeserializer() {
    this.classLoader = null;
  }

  /**
   * Create a {@code DefaultDeserializer} for using an {@link ObjectInputStream}
   * with the given {@code ClassLoader}.
   *
   * @see ConfigurableObjectInputStream#ConfigurableObjectInputStream(InputStream, ClassLoader)
   */
  public DefaultDeserializer(@Nullable ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  /**
   * Read from the supplied {@code InputStream} and deserialize the contents
   * into an object.
   *
   * @see ObjectInputStream#readObject()
   */
  @Override
  public Object deserialize(InputStream inputStream) throws IOException {
    var objectInputStream = new ConfigurableObjectInputStream(inputStream, this.classLoader);
    try {
      return objectInputStream.readObject();
    }
    catch (ClassNotFoundException ex) {
      throw new IOException("Failed to deserialize object type", ex);
    }
  }

}
