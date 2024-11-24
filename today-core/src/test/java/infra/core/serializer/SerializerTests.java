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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import infra.core.serializer.support.SerializationDelegate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Unit tests for {@link Serializer}, {@link Deserializer}, and {@link SerializationDelegate}.
 */
class SerializerTests {

  private static final String FRAMEWORK = "TODAY INFRA";

  @Test
  void serializeToByteArray() throws IOException {

    class SpyStringSerializer implements Serializer<String> {

      String expectedObject;
      OutputStream expectedOutputStream;

      @Override
      public void serialize(String object, OutputStream outputStream) {
        this.expectedObject = object;
        this.expectedOutputStream = outputStream;
      }
    }

    SpyStringSerializer serializer = new SpyStringSerializer();
    serializer.serializeToByteArray(FRAMEWORK);
    assertThat(serializer.expectedObject).isEqualTo(FRAMEWORK);
    assertThat(serializer.expectedOutputStream).isNotNull();
  }

  @Test
  void deserializeToByteArray() throws IOException {

    class SpyStringDeserializer implements Deserializer<String> {

      InputStream expectedInputStream;

      @Override
      public String deserialize(InputStream inputStream) {
        this.expectedInputStream = inputStream;
        return FRAMEWORK;
      }
    }

    SpyStringDeserializer deserializer = new SpyStringDeserializer();
    Object deserializedObj = deserializer.deserializeFromByteArray(FRAMEWORK.getBytes());
    assertThat(deserializedObj).isEqualTo(FRAMEWORK);
    assertThat(deserializer.expectedInputStream).isNotNull();
  }

  @Test
  void serializationDelegateWithExplicitSerializerAndDeserializer() throws IOException {
    SerializationDelegate delegate = new SerializationDelegate(new DefaultSerializer(), new DefaultDeserializer());
    byte[] serializedObj = delegate.serializeToByteArray(FRAMEWORK);
    Object deserializedObj = delegate.deserialize(new ByteArrayInputStream(serializedObj));
    assertThat(deserializedObj).isEqualTo(FRAMEWORK);
  }

  @Test
  void serializationDelegateWithExplicitClassLoader() throws IOException {
    SerializationDelegate delegate = new SerializationDelegate(getClass().getClassLoader());
    byte[] serializedObj = delegate.serializeToByteArray(FRAMEWORK);
    Object deserializedObj = delegate.deserialize(new ByteArrayInputStream(serializedObj));
    assertThat(deserializedObj).isEqualTo(FRAMEWORK);
  }

}
