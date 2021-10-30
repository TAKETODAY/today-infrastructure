/*
 * Copyright 2002-2021 the original author or authors.
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

package cn.taketoday.core.type.classreading;

import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * {@link MethodMetadata} created from a {@link SimpleMethodMetadataReadingVisitor}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 */
final class SimpleMethodMetadata implements MethodMetadata {

  private final String methodName;

  private final int access;

  private final String declaringClassName;

  private final String returnTypeName;

  // The source implements equals(), hashCode(), and toString() for the underlying method.
  private final Object source;

  private final MergedAnnotations annotations;

  private final int parameterCount;
  private final Type[] argumentTypes;

  @Nullable
  private final ClassLoader classLoader;

  SimpleMethodMetadata(String methodName, int access, String declaringClassName,
                       String returnTypeName, Object source, MergedAnnotations annotations,
                       Type[] argumentTypes, @Nullable ClassLoader classLoader) {

    this.methodName = methodName;
    this.access = access;
    this.declaringClassName = declaringClassName;
    this.returnTypeName = returnTypeName;
    this.source = source;
    this.annotations = annotations;
    this.parameterCount = argumentTypes.length;
    this.argumentTypes = argumentTypes;
    this.classLoader = classLoader;
  }

  @Override
  public String getMethodName() {
    return this.methodName;
  }

  @Override
  public String getDeclaringClassName() {
    return this.declaringClassName;
  }

  @Override
  public String getReturnTypeName() {
    return this.returnTypeName;
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

  @Override
  public int getParameterCount() {
    return parameterCount;
  }

  private boolean isPrivate() {
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

  @Override
  public Type[] getArgumentTypes() {
    return argumentTypes;
  }

  @Override
  public Class<?>[] getParameterTypes() {
    if (parameterCount == 0) {
      return Constant.EMPTY_CLASS_ARRAY;
    }
    int i = 0;
    Class<?>[] ret = new Class<?>[parameterCount];
    for (Type argumentType : argumentTypes) {
      String className = argumentType.getClassName();
      ret[i++] = ClassUtils.resolveClassName(className, classLoader);
    }
    return ret;
  }

}
