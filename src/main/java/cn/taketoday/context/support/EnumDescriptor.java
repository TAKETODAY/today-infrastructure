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

import cn.taketoday.asm.Type;
import cn.taketoday.context.utils.ClassUtils;

/**
 * @author TODAY 2021/7/28 22:41
 * @since 4.0
 */
public final class EnumDescriptor implements AnnotationValueCapable {

  final String value;
  final String descriptor;

  public EnumDescriptor(String value, String descriptor) {
    this.value = value;
    this.descriptor = descriptor;
  }

  public String getValue() {
    return value;
  }

  public String getDescriptor() {
    return descriptor;
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public Enum getAnnotationValue() {
    final String className = Type.fromDescriptor(descriptor).getClassName();
    final Class<Enum> enumClass = ClassUtils.loadClass(className);
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
