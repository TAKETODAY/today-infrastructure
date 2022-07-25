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

package cn.taketoday.beans.testfixture;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class SerializationTestUtils {

  public static void testSerialization(Object o) throws IOException {
    OutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(o);
    }
  }

  public static boolean isSerializable(Object o) throws IOException {
    try {
      testSerialization(o);
      return true;
    }
    catch (NotSerializableException ex) {
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T serializeAndDeserialize(T o) throws IOException, ClassNotFoundException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(o);
      oos.flush();
    }
    byte[] bytes = baos.toByteArray();

    ByteArrayInputStream is = new ByteArrayInputStream(bytes);
    try (ObjectInputStream ois = new ObjectInputStream(is)) {
      return (T) ois.readObject();
    }
  }

  public static <T> T serializeAndDeserialize(Object o, Class<T> expectedType) throws IOException, ClassNotFoundException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(o);
      oos.flush();
    }
    byte[] bytes = baos.toByteArray();

    ByteArrayInputStream is = new ByteArrayInputStream(bytes);
    try (ObjectInputStream ois = new ObjectInputStream(is)) {
      return expectedType.cast(ois.readObject());
    }
  }

}

