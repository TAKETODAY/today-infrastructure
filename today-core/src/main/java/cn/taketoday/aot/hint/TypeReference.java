/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.aot.hint;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.lang.Nullable;

/**
 * Type abstraction that can be used to refer to types that are not available as
 * a {@link Class} yet.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface TypeReference extends Comparable<TypeReference> {

  /**
   * Return the fully qualified name of this type reference.
   *
   * @return the reflection target name
   */
  String getName();

  /**
   * Return the {@linkplain Class#getCanonicalName() canonical name} of this
   * type reference.
   *
   * @return the canonical name
   */
  String getCanonicalName();

  /**
   * Return the package name of this type.
   *
   * @return the package name
   */
  String getPackageName();

  /**
   * Return the {@linkplain Class#getSimpleName() simple name} of this type
   * reference.
   *
   * @return the simple name
   */
  String getSimpleName();

  /**
   * Return the enclosing type reference, or {@code null} if this type reference
   * does not have an enclosing type.
   *
   * @return the enclosing type, if any
   */
  @Nullable
  TypeReference getEnclosingType();

  /**
   * Create an instance based on the specified type.
   *
   * @param type the type to wrap
   * @return a type reference for the specified type
   * @throws IllegalArgumentException if the specified type {@linkplain Class#getCanonicalName() canonical name} is {@code null}
   */
  static TypeReference of(Class<?> type) {
    return ReflectionTypeReference.of(type);
  }

  /**
   * Create an instance based on the specified class name.
   * The format of the class name must follow {@linkplain Class#getName()},
   * in particular inner classes should be separated by a {@code $}.
   *
   * @param className the class name of the type to wrap
   * @return a type reference for the specified class name
   */
  static TypeReference of(String className) {
    return SimpleTypeReference.of(className);
  }

  /**
   * Create a list of {@link TypeReference type references} mapped by the specified
   * types.
   *
   * @param types the types to map
   * @return a list of type references
   */
  static List<TypeReference> listOf(Class<?>... types) {
    return Arrays.stream(types).map(TypeReference::of).toList();
  }

}
