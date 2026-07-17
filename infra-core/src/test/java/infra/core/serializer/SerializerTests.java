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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import infra.core.serializer.support.SerializationDelegate;

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
    Assertions.assertThat(serializer.expectedObject).isEqualTo(FRAMEWORK);
    Assertions.assertThat(serializer.expectedOutputStream).isNotNull();
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
    Assertions.assertThat(deserializedObj).isEqualTo(FRAMEWORK);
    Assertions.assertThat(deserializer.expectedInputStream).isNotNull();
  }

  @Test
  void defaultDeserializerExposesNullClassLoaderByDefault() {
    Assertions.assertThat(new DefaultDeserializer().getClassLoader()).isNull();
  }

  @Test
  void defaultDeserializerExposesConfiguredClassLoader() {
    ClassLoader classLoader = getClass().getClassLoader();
    Assertions.assertThat(new DefaultDeserializer(classLoader).getClassLoader()).isSameAs(classLoader);
  }

  @Test
  void serializationDelegateWithExplicitSerializerAndDeserializer() throws IOException {
    SerializationDelegate delegate = new SerializationDelegate(new DefaultSerializer(), new DefaultDeserializer());
    byte[] serializedObj = delegate.serializeToByteArray(FRAMEWORK);
    Object deserializedObj = delegate.deserialize(new ByteArrayInputStream(serializedObj));
    Assertions.assertThat(deserializedObj).isEqualTo(FRAMEWORK);
  }

  @Test
  void serializationDelegateWithExplicitClassLoader() throws IOException {
    SerializationDelegate delegate = new SerializationDelegate(getClass().getClassLoader());
    byte[] serializedObj = delegate.serializeToByteArray(FRAMEWORK);
    Object deserializedObj = delegate.deserialize(new ByteArrayInputStream(serializedObj));
    Assertions.assertThat(deserializedObj).isEqualTo(FRAMEWORK);
  }

}
