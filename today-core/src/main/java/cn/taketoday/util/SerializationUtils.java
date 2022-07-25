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
import java.io.Serializable;

import cn.taketoday.lang.Nullable;

/**
 * Static utilities for serialization and deserialization using
 * <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/serialization/"
 * target="_blank">Java Object Serialization</a>.
 *
 * <p><strong>WARNING</strong>: These utilities should be used with caution. See
 * <a href="https://www.oracle.com/java/technologies/javase/seccodeguide.html#8"
 * target="_blank">Secure Coding Guidelines for the Java Programming Language</a>
 * for details.
 *
 * @author Dave Syer
 * @author Loïc Ledoyen
 * @author Sam Brannen
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
   * @deprecated This utility uses Java Object Serialization, which allows
   * arbitrary code to be run and is known for being the source of many Remote
   * Code Execution (RCE) vulnerabilities.
   * <p>Prefer the use of an external tool (that serializes to JSON, XML, or
   * any other format) which is regularly checked and updated for not allowing RCE.
   */
  @Deprecated
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

  /**
   * Clone the given object using Java Object Serialization.
   *
   * @param object the object to clone
   * @param <T> the type of the object to clone
   * @return a clone (deep-copy) of the given object
   * @since 6.0
   */
  @SuppressWarnings("unchecked")
  public static <T extends Serializable> T clone(T object) {
    return (T) SerializationUtils.deserialize(SerializationUtils.serialize(object));
  }

}
