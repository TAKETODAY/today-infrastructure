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

/**
 * @author TODAY 2021/7/28 22:41
 * @since 4.0
 */
public final class EnumValueHolder extends AnnotationValueHolder {

  final String name;

  final String descriptor;

  public EnumValueHolder(String descriptor, String name) {
    this.name = name;
    this.descriptor = descriptor;
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected Object getInternal() {
    Class enumClass = ClassValueHolder.fromDescriptor(descriptor).read();
    return Enum.valueOf(enumClass, name);
  }

  @Override
  public void write(ByteVector annotation, SymbolTable symbolTable) {
    annotation.put12('e', symbolTable.addConstantUtf8(descriptor))
            .putShort(symbolTable.addConstantUtf8(name));
  }

  public String getName() {
    return name;
  }

  public String getDescriptor() {
    return descriptor;
  }

  @Override
  public String toString() {
    return "EnumValueHolder{value='%s', descriptor='%s'}".formatted(name, descriptor);
  }
}
