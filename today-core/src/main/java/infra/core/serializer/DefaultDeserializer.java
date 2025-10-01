/*
 * Copyright 2017 - 2025 the original author or authors.
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
