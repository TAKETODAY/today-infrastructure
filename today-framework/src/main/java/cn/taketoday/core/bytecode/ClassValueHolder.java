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

package cn.taketoday.core.bytecode;

import cn.taketoday.util.ClassUtils;

/**
 * @author TODAY 2021/7/28 23:06
 */
public final class ClassValueHolder extends AnnotationValueHolder {
  private final Type descriptor;

  public ClassValueHolder(String descriptor) {
    this.descriptor = Type.fromDescriptor(descriptor);
  }

  public ClassValueHolder(Type descriptor) {
    this.descriptor = descriptor;
  }

  @Override
  public Class<?> getInternal() {
    return ClassUtils.load(descriptor.getClassName());
  }

  @Override
  public Class<?> read() {
    return (Class<?>) super.read();
  }

  @Override
  public void write(ByteVector annotation, SymbolTable symbolTable) {
    annotation.put12('c', symbolTable.addConstantUtf8(descriptor.getDescriptor()));
  }

  public static ClassValueHolder fromDescriptor(final String typeDescriptor) {
    return new ClassValueHolder(typeDescriptor);
  }

  public Type getDescriptor() {
    return descriptor;
  }

  @Override
  public String toString() {
    return "ClassValueHolder{" +
            "descriptor=" + descriptor +
            '}';
  }
}
