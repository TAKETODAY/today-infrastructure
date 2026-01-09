/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core.type.classreading;

import org.jspecify.annotations.Nullable;

import infra.bytecode.Opcodes;
import infra.core.annotation.MergedAnnotations;
import infra.core.type.MethodMetadata;

/**
 * {@link MethodMetadata} created from a {@link SimpleMethodMetadataReadingVisitor}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class SimpleMethodMetadata implements MethodMetadata {

  final int access;

  private final String declaringClassName;

  // The source implements equals(), hashCode(), and toString() for the underlying method.
  private final Object source;

  private final MergedAnnotations annotations;

  private final String methodName;

  private final String returnTypeName;

  SimpleMethodMetadata(String methodName, int access, String declaringClassName,
          String returnTypeName, Object source, MergedAnnotations annotations) {

    this.source = source;
    this.methodName = methodName;
    this.access = access;
    this.declaringClassName = declaringClassName;
    this.returnTypeName = returnTypeName;
    this.annotations = annotations;
  }

  @Override
  public String getMethodName() {
    return methodName;
  }

  @Override
  public String getDeclaringClassName() {
    return this.declaringClassName;
  }

  @Override
  public String getReturnTypeName() {
    return returnTypeName;
  }

  @Override
  public boolean isAbstract() {
    return (this.access & Opcodes.ACC_ABSTRACT) != 0;
  }

  @Override
  public boolean isStatic() {
    return (this.access & Opcodes.ACC_STATIC) != 0;
  }

  @Override
  public boolean isFinal() {
    return (this.access & Opcodes.ACC_FINAL) != 0;
  }

  @Override
  public boolean isOverridable() {
    return !isStatic() && !isFinal() && !isPrivate();
  }

  boolean isPrivate() {
    return (this.access & Opcodes.ACC_PRIVATE) != 0;
  }

  @Override
  public MergedAnnotations getAnnotations() {
    return this.annotations;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return ((this == obj) || ((obj instanceof SimpleMethodMetadata) &&
            this.source.equals(((SimpleMethodMetadata) obj).source)));
  }

  @Override
  public int hashCode() {
    return this.source.hashCode();
  }

  @Override
  public String toString() {
    return this.source.toString();
  }

}
