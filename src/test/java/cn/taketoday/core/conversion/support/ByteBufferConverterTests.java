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

package cn.taketoday.core.conversion.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import cn.taketoday.core.conversion.Converter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ByteBufferConverter}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 */
class ByteBufferConverterTests {

  private GenericConversionService conversionService;

  @BeforeEach
  void setup() {
    this.conversionService = new DefaultConversionService();
    this.conversionService.addConverter(new ByteArrayToOtherTypeConverter());
    this.conversionService.addConverter(new OtherTypeToByteArrayConverter());
  }

  @Test
  void byteArrayToByteBuffer() throws Exception {
    byte[] bytes = new byte[] { 1, 2, 3 };
    ByteBuffer convert = this.conversionService.convert(bytes, ByteBuffer.class);
    assertThat(convert.array()).isNotSameAs(bytes);
    assertThat(convert.array()).isEqualTo(bytes);
  }

  @Test
  void byteBufferToByteArray() throws Exception {
    byte[] bytes = new byte[] { 1, 2, 3 };
    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
    byte[] convert = this.conversionService.convert(byteBuffer, byte[].class);
    assertThat(convert).isNotSameAs(bytes);
    assertThat(convert).isEqualTo(bytes);
  }

  @Test
  void byteBufferToOtherType() throws Exception {
    byte[] bytes = new byte[] { 1, 2, 3 };
    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
    OtherType convert = this.conversionService.convert(byteBuffer, OtherType.class);
    assertThat(convert.bytes).isNotSameAs(bytes);
    assertThat(convert.bytes).isEqualTo(bytes);
  }

  @Test
  void otherTypeToByteBuffer() throws Exception {
    byte[] bytes = new byte[] { 1, 2, 3 };
    OtherType otherType = new OtherType(bytes);
    ByteBuffer convert = this.conversionService.convert(otherType, ByteBuffer.class);
    assertThat(convert.array()).isNotSameAs(bytes);
    assertThat(convert.array()).isEqualTo(bytes);
  }

  @Test
  void byteBufferToByteBuffer() throws Exception {
    byte[] bytes = new byte[] { 1, 2, 3 };
    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
    ByteBuffer convert = this.conversionService.convert(byteBuffer, ByteBuffer.class);
    assertThat(convert).isNotSameAs(byteBuffer.rewind());
    assertThat(convert).isEqualTo(byteBuffer.rewind());
    assertThat(convert).isEqualTo(ByteBuffer.wrap(bytes));
    assertThat(convert.array()).isEqualTo(bytes);
  }

  private static class OtherType {

    private byte[] bytes;

    public OtherType(byte[] bytes) {
      this.bytes = bytes;
    }

  }

  private static class ByteArrayToOtherTypeConverter implements Converter<byte[], OtherType> {

    @Override
    public OtherType convert(byte[] source) {
      return new OtherType(source);
    }
  }

  private static class OtherTypeToByteArrayConverter implements Converter<OtherType, byte[]> {

    @Override
    public byte[] convert(OtherType source) {
      return source.bytes;
    }

  }

}
