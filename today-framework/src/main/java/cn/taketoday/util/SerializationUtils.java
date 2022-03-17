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

package cn.taketoday.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import cn.taketoday.lang.Nullable;

/**
 * Static utilities for serialization and deserialization.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/9 21:17
 */
public abstract class SerializationUtils {

  /**
   * Serialize the given object to a byte array.
   *
   * @param object the object to serialize
   * @return an array of bytes representing the object in a portable fashion
   */
  @Nullable
  public static byte[] serialize(@Nullable Object object) {
    if (object == null) {
      return null;
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(object);
      oos.flush();
    }
    catch (IOException ex) {
      throw new IllegalArgumentException("Failed to serialize object of type: " + object.getClass(), ex);
    }
    return baos.toByteArray();
  }

  /**
   * Deserialize the byte array into an object.
   *
   * @param bytes a serialized object
   * @return the result of deserializing the bytes
   */
  @Nullable
  public static Object deserialize(@Nullable byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return ois.readObject();
    }
    catch (IOException ex) {
      throw new IllegalArgumentException("Failed to deserialize object", ex);
    }
    catch (ClassNotFoundException ex) {
      throw new IllegalStateException("Failed to deserialize object type", ex);
    }
  }

}
