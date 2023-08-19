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

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.NotSerializableException;
import java.io.Serializable;

import cn.taketoday.core.ConfigurableObjectInputStream;
import cn.taketoday.core.serializer.support.DeserializingConverter;
import cn.taketoday.core.serializer.support.SerializationFailedException;
import cn.taketoday.core.serializer.support.SerializingConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.BDDMockito.given;

/**
 * @author Gary Russell
 * @author Mark Fisher
 * @since 4.0
 */
class SerializationConverterTests {

  @Test
  void serializeAndDeserializeStringWithDefaultSerializer() {
    SerializingConverter toBytes = new SerializingConverter();
    byte[] bytes = toBytes.convert("Testing");
    DeserializingConverter fromBytes = new DeserializingConverter();
    assertThat(fromBytes.convert(bytes)).isEqualTo("Testing");
  }

  @Test
  void serializeAndDeserializeStringWithExplicitSerializer() {
    SerializingConverter toBytes = new SerializingConverter(new DefaultSerializer());
    byte[] bytes = toBytes.convert("Testing");
    DeserializingConverter fromBytes = new DeserializingConverter();
    assertThat(fromBytes.convert(bytes)).isEqualTo("Testing");
  }

  @Test
  void nonSerializableObject() {
    SerializingConverter toBytes = new SerializingConverter();
    assertThatExceptionOfType(SerializationFailedException.class)
            .isThrownBy(() -> toBytes.convert(new Object()))
            .havingCause()
            .isInstanceOf(IllegalArgumentException.class)
            .withMessageContaining("requires a Serializable payload");
  }

  @Test
  void nonSerializableField() {
    SerializingConverter toBytes = new SerializingConverter();
    assertThatExceptionOfType(SerializationFailedException.class)
            .isThrownBy(() -> toBytes.convert(new UnSerializable()))
            .withCauseInstanceOf(NotSerializableException.class);
  }

  @Test
  void deserializationFailure() {
    DeserializingConverter fromBytes = new DeserializingConverter();
    assertThatExceptionOfType(SerializationFailedException.class)
            .isThrownBy(() -> fromBytes.convert("Junk".getBytes()));
  }

  @Test
  void deserializationWithExplicitClassLoader() {
    DeserializingConverter fromBytes = new DeserializingConverter(getClass().getClassLoader());
    SerializingConverter toBytes = new SerializingConverter();
    String expected = "TODAY FRAMEWORK";
    assertThat(fromBytes.convert(toBytes.convert(expected))).isEqualTo(expected);
  }

  @Test
  void deserializationWithExplicitDeserializer() {
    DeserializingConverter fromBytes = new DeserializingConverter(new DefaultDeserializer());
    SerializingConverter toBytes = new SerializingConverter();
    String expected = "TODAY FRAMEWORK";
    assertThat(fromBytes.convert(toBytes.convert(expected))).isEqualTo(expected);
  }

  @Test
  void deserializationIOException() {
    ClassNotFoundException classNotFoundException = new ClassNotFoundException();
    try (MockedConstruction<ConfigurableObjectInputStream> mocked =
            Mockito.mockConstruction(ConfigurableObjectInputStream.class,
                    (mock, context) -> given(mock.readObject()).willThrow(classNotFoundException))) {
      DefaultDeserializer defaultSerializer = new DefaultDeserializer(getClass().getClassLoader());
      assertThat(mocked).isNotNull();
      assertThatIOException()
              .isThrownBy(() -> defaultSerializer.deserialize(new ByteArrayInputStream("test".getBytes())))
              .withMessage("Failed to deserialize object type")
              .havingCause().isSameAs(classNotFoundException);
    }
  }

  static class UnSerializable implements Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings({ "unused", "serial" })
    private Object object = new Object();
  }

}
