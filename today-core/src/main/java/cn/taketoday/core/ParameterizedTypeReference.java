/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
 * <pre> {@code
 * ParameterizedTypeReference<List<String>> typeRef = new ParameterizedTypeReference<List<String>>() {};
 * }</pre>
 *
 * <p>The resulting {@code typeRef} instance can then be used to obtain a {@link Type}
 * instance that carries the captured parameterized type information at runtime.
 * For more information on "super type tokens" see the link to Neal Gafter's blog post.
 *
 * @param <T> the referenced type
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://gafter.blogspot.nl/2006/12/super-type-tokens.html">Neal Gafter on Super Type Tokens</a>
 * @since 3.0 2021/1/6 22:11
 */
public abstract class ParameterizedTypeReference<T> {
  private final Type type;

  protected ParameterizedTypeReference() {
    Class<?> typeReferenceSubclass = findTypeReferenceSubclass(getClass());
    Type type = typeReferenceSubclass.getGenericSuperclass();
    Assert.isInstanceOf(ParameterizedType.class, type, "Type must be a parameterized type");
    ParameterizedType parameterizedType = (ParameterizedType) type;
    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
    Assert.isTrue(actualTypeArguments.length == 1, "Number of type arguments must be 1");
    this.type = actualTypeArguments[0];
  }

  private ParameterizedTypeReference(Type type) {
    this.type = type;
  }

  public final ResolvableType getResolvableType() {
    return ResolvableType.forType(getType());
  }

  public final Type getType() {
    return this.type;
  }

  @Override
  public boolean equals(Object other) {
    return (this == other
            || (other instanceof ParameterizedTypeReference && this.type.equals(((ParameterizedTypeReference<?>) other).type)));
  }

  @Override
  public int hashCode() {
    return this.type.hashCode();
  }

  @Override
  public String toString() {
    return "ParameterizedTypeReference<" + this.type + ">";
  }

  /**
   * Build a {@code TypeReference} wrapping the given type.
   *
   * @param type a generic type (possibly obtained via reflection,
   * e.g. from {@link java.lang.reflect.Method#getGenericReturnType()})
   * @return a corresponding reference which may be passed into
   * {@code TypeReference}-accepting methods
   */
  public static <T> ParameterizedTypeReference<T> forType(Type type) {
    return new ParameterizedTypeReference<T>(type) { };
  }

  private static Class<?> findTypeReferenceSubclass(Class<?> child) {
    Class<?> parent = child.getSuperclass();
    if (Object.class == parent) {
      throw new IllegalStateException("Expected TypeReference superclass");
    }
    else if (ParameterizedTypeReference.class == parent) {
      return child;
    }
    else {
      return findTypeReferenceSubclass(parent);
    }
  }
}
