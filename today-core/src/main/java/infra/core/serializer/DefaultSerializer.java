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


package infra.core.serializer;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * A {@link Serializer} implementation that writes an object to an output stream
 * using Java serialization.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @since 4.0
 */
public class DefaultSerializer implements Serializer<Object> {

  /**
   * Writes the source object to an output stream using Java serialization.
   * The source object must implement {@link Serializable}.
   *
   * @see ObjectOutputStream#writeObject(Object)
   */
  @Override
  public void serialize(Object object, OutputStream outputStream) throws IOException {
    if (!(object instanceof Serializable)) {
      throw new IllegalArgumentException(getClass().getSimpleName() + " requires a Serializable payload " +
              "but received an object of type [" + object.getClass().getName() + "]");
    }
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
    objectOutputStream.writeObject(object);
    objectOutputStream.flush();
  }

}
