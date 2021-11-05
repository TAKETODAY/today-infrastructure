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
