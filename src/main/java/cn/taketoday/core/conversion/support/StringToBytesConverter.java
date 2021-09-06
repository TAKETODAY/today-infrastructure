/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import cn.taketoday.util.TypeDescriptor;

/**
 * convert String to byte array
 *
 * @author TODAY 2021/5/13 22:18
 * @since 3.0.1
 */
public final class StringToBytesConverter extends ToArrayConverter {
  private final Charset charset;

  public StringToBytesConverter() {
    this(StandardCharsets.UTF_8);
  }

  public StringToBytesConverter(Charset charset) {
    this.charset = charset;
  }

  @Override
  public Object convert(TypeDescriptor targetType, Object source) {
    // convert String to byte array
    return ((String) source).getBytes(charset);
  }

  @Override
  protected boolean supportsInternal(TypeDescriptor targetType, Class<?> sourceType) {
    return sourceType == String.class && targetType.is(byte[].class);
  }
}
