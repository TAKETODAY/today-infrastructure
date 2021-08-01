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

package cn.taketoday.context.support;

/**
 * @author TODAY 2021/7/28 22:41
 * @since 4.0
 */
public final class EnumValue implements AnnotationValue {

  final String value;
  final String descriptor;

  public EnumValue(String value, String descriptor) {
    this.value = value;
    this.descriptor = descriptor;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public Enum get() {
    final Class enumClass = ClassValue.fromDescriptor(descriptor).get();
    return Enum.valueOf(enumClass, value);
  }

  @Override
  public String toString() {
    return "EnumDescriptor{" +
            "value='" + value + '\'' +
            ", descriptor='" + descriptor + '\'' +
            '}';
  }
}
