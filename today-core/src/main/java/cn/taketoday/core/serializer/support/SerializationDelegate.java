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

package cn.taketoday.core.serializer.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cn.taketoday.core.serializer.DefaultDeserializer;
import cn.taketoday.core.serializer.DefaultSerializer;
import cn.taketoday.core.serializer.Deserializer;
import cn.taketoday.core.serializer.Serializer;
import cn.taketoday.lang.Assert;

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
