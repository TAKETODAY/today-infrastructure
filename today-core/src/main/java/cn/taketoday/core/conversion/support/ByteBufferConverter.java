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

import java.nio.ByteBuffer;
import java.util.Set;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConditionalGenericConverter;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;

/**
 * Converts a {@link ByteBuffer} directly to and from {@code byte[] ByteBuffer} directly to and from {@code byte[]s} and indirectly
 * to any type that the {@link ConversionService} support via {@code byte[]}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 4.0
 */
final class ByteBufferConverter implements ConditionalGenericConverter {

  private static final TypeDescriptor BYTE_ARRAY_TYPE = TypeDescriptor.valueOf(byte[].class);
  private static final TypeDescriptor BYTE_BUFFER_TYPE = TypeDescriptor.valueOf(ByteBuffer.class);

  private static final Set<ConvertiblePair> CONVERTIBLE_PAIRS = Set.of(
          new ConvertiblePair(ByteBuffer.class, byte[].class),
          new ConvertiblePair(byte[].class, ByteBuffer.class),
          new ConvertiblePair(ByteBuffer.class, Object.class),
          new ConvertiblePair(Object.class, ByteBuffer.class)
  );

  private final ConversionService conversionService;

  public ByteBufferConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return CONVERTIBLE_PAIRS;
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    boolean byteBufferTarget = targetType.isAssignableTo(BYTE_BUFFER_TYPE);
    if (sourceType.isAssignableTo(BYTE_BUFFER_TYPE)) {
      return byteBufferTarget || matchesFromByteBuffer(targetType);
    }
    return byteBufferTarget && matchesToByteBuffer(sourceType);
  }

  private boolean matchesFromByteBuffer(TypeDescriptor targetType) {
    return targetType.isAssignableTo(BYTE_ARRAY_TYPE)
            || conversionService.canConvert(BYTE_ARRAY_TYPE, targetType);
  }

  private boolean matchesToByteBuffer(TypeDescriptor sourceType) {
    return sourceType.isAssignableTo(BYTE_ARRAY_TYPE)
            || conversionService.canConvert(sourceType, BYTE_ARRAY_TYPE);
  }

  @Override
  @Nullable
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    boolean byteBufferTarget = targetType.isAssignableTo(BYTE_BUFFER_TYPE);
    if (source instanceof ByteBuffer buffer) {
      return byteBufferTarget ? buffer.duplicate() : convertFromByteBuffer(buffer, targetType);
    }
    if (byteBufferTarget) {
      return convertToByteBuffer(source, sourceType);
    }
    // Should not happen
    throw new IllegalStateException("Unexpected source/target types");
  }

  @Nullable
  private Object convertFromByteBuffer(ByteBuffer source, TypeDescriptor targetType) {
    byte[] bytes = new byte[source.remaining()];
    source.get(bytes);

    if (targetType.isAssignableTo(BYTE_ARRAY_TYPE)) {
      return bytes;
    }
    return conversionService.convert(bytes, BYTE_ARRAY_TYPE, targetType);
  }

  private Object convertToByteBuffer(@Nullable Object source, TypeDescriptor sourceType) {
    byte[] bytes = (byte[]) (source instanceof byte[] ? source :
                             conversionService.convert(source, sourceType, BYTE_ARRAY_TYPE));

    if (bytes == null) {
      return ByteBuffer.wrap(Constant.EMPTY_BYTES);
    }

    ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
    byteBuffer.put(bytes);

    return byteBuffer.rewind();
  }

}
