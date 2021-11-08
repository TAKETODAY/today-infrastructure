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

package cn.taketoday.core;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import cn.taketoday.lang.Assert;

/**
 * The purpose of this class is to enable capturing and passing a generic
 * {@link Type}. In order to capture the generic type and retain it at runtime,
 * you need to create a subclass (ideally as anonymous inline class) as follows:
 *
 * <pre class="code">
 * TypeReference&lt;List&lt;String&gt;&gt; typeRef = new TypeReference&lt;List&lt;String&gt;&gt;() {};
 * </pre>
 *
 * @param <T> the referenced type
 * @author TODAY 2021/1/6 22:11
 * @since 3.0
 */
public abstract class TypeReference<T> {
  private final Type type;

  protected TypeReference() {
    Class<?> TypeReferenceSubclass = findTypeReferenceSubclass(getClass());
    Type type = TypeReferenceSubclass.getGenericSuperclass();
    Assert.isInstanceOf(ParameterizedType.class, type, "Type must be a parameterized type");
    ParameterizedType parameterizedType = (ParameterizedType) type;
    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
    Assert.isTrue(actualTypeArguments.length == 1, "Number of type arguments must be 1");
    this.type = actualTypeArguments[0];
  }

  private TypeReference(Type type) {
    this.type = type;
  }

  public final ResolvableType getResolvableType() {
    return ResolvableType.fromType(getType());
  }

  public final Type getType() {
    return this.type;
  }

  @Override
  public boolean equals(Object other) {
    return (this == other
            || (other instanceof TypeReference && this.type.equals(((TypeReference<?>) other).type)));
  }

  @Override
  public int hashCode() {
    return this.type.hashCode();
  }

  @Override
  public String toString() {
    return "TypeReference<" + this.type + ">";
  }

  /**
   * Build a {@code TypeReference} wrapping the given type.
   *
   * @param type a generic type (possibly obtained via reflection,
   * e.g. from {@link java.lang.reflect.Method#getGenericReturnType()})
   * @return a corresponding reference which may be passed into
   * {@code TypeReference}-accepting methods
   */
  public static <T> TypeReference<T> fromType(Type type) {
    return new TypeReference<T>(type) { };
  }

  private static Class<?> findTypeReferenceSubclass(Class<?> child) {
    Class<?> parent = child.getSuperclass();
    if (Object.class == parent) {
      throw new IllegalStateException("Expected TypeReference superclass");
    }
    else if (TypeReference.class == parent) {
      return child;
    }
    else {
      return findTypeReferenceSubclass(parent);
    }
  }
}
