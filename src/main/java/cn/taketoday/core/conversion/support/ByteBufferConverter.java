/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.nio.ByteBuffer;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.MatchingConverter;

/**
 * Converts a {@link ByteBuffer} directly to and from {@code byte[] ByteBuffer}
 * directly to and from {@code byte[]s} and indirectly to any type
 * that the {@link ConversionService} support via {@code byte[]}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author TODAY
 * @since 3.0
 */
final class ByteBufferConverter implements MatchingConverter {
  private final ConversionService conversionService;

  public ByteBufferConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public boolean supports(TypeDescriptor targetType, Class<?> sourceType) {
    // ByteBuffer.class -> byte[].class
    // ByteBuffer.class -> Object.class
    // byte[].class -> ByteBuffer.class
    // Object.class -> ByteBuffer.class

    final boolean byteBufferTarget = targetType.isAssignableTo(ByteBuffer.class);
    if (ByteBuffer.class.isAssignableFrom(sourceType)) {
      // 转换为其他ByteBuffer
      return byteBufferTarget ||
              (targetType.is(byte[].class) || conversionService.canConvert(sourceType, targetType));
    }

    return byteBufferTarget && matchesToByteBuffer(sourceType);
  }

  private boolean matchesToByteBuffer(Class<?> sourceType) {
    return (sourceType == byte[].class || conversionService.canConvert(sourceType, byte[].class));
  }

  @Override
  public Object convert(TypeDescriptor targetType, Object source) {
    boolean byteBufferTarget = targetType.isAssignableTo(ByteBuffer.class);
    if (source instanceof ByteBuffer buffer) {
      return (byteBufferTarget ? buffer.duplicate() : convertFromByteBuffer(buffer, targetType));
    }
    if (byteBufferTarget) {
      return convertToByteBuffer(source);
    }
    // Should not happen
    throw new IllegalStateException("Unexpected source/target types");
  }

  private Object convertFromByteBuffer(ByteBuffer source, TypeDescriptor targetType) {
    byte[] bytes = new byte[source.remaining()];
    source.get(bytes);
    if (targetType.is(byte[].class)) {
      return bytes;
    }
    return this.conversionService.convert(bytes, targetType);
  }

  private Object convertToByteBuffer(Object source) {
    byte[] bytes = (byte[]) (source instanceof byte[] ? source : conversionService.convert(source, byte[].class));

    if (bytes == null) {
      return ByteBuffer.wrap(new byte[0]);
    }

    ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
    byteBuffer.put(bytes);

    // Extra cast necessary for compiling on JDK 9 plus running on JDK 8, since
    // otherwise the overridden ByteBuffer-returning rewind method would be chosen
    // which isn't available on JDK 8.
    return byteBuffer.rewind();
  }

}
