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

import infra.util.ClassUtils;

/**
 * @author TODAY 2021/7/28 23:06
 */
public final class ClassValueHolder extends AnnotationValueHolder {
  private final Type descriptor;

  public ClassValueHolder(String descriptor) {
    this.descriptor = Type.forDescriptor(descriptor);
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
