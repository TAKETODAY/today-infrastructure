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

package cn.taketoday.bytecode;

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
  protected Class<?> getInternal() {
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
    return "ClassValueHolder{descriptor=%s}".formatted(descriptor);
  }
}
