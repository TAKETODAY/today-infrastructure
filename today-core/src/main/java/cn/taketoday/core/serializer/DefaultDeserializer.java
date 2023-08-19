/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import cn.taketoday.core.ConfigurableObjectInputStream;
import cn.taketoday.lang.Nullable;

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
    try (ObjectInputStream objectInputStream = new ConfigurableObjectInputStream(inputStream, this.classLoader)) {
      return objectInputStream.readObject();
    }
    catch (ClassNotFoundException ex) {
      throw new IOException("Failed to deserialize object type", ex);
    }
  }

}
