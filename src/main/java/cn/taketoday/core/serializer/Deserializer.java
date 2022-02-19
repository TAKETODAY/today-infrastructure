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

package cn.taketoday.core.serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A strategy interface for converting from data in an InputStream to an Object.
 *
 * @param <T> the object type
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @see Serializer
 * @since 4.0
 */
@FunctionalInterface
public interface Deserializer<T> {

  /**
   * Read (assemble) an object of type T from the given InputStream.
   * <p>Note: Implementations should not close the given InputStream
   * (or any decorators of that InputStream) but rather leave this up
   * to the caller.
   *
   * @param inputStream the input stream
   * @return the deserialized object
   * @throws IOException in case of errors reading from the stream
   */
  T deserialize(InputStream inputStream) throws IOException;

  /**
   * Read (assemble) an object of type T from the given byte array.
   *
   * @param serialized the byte array
   * @return the deserialized object
   * @throws IOException in case of deserialization failure
   */
  default T deserializeFromByteArray(byte[] serialized) throws IOException {
    return deserialize(new ByteArrayInputStream(serialized));
  }

}
