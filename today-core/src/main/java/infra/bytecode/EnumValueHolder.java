/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.bytecode;

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
