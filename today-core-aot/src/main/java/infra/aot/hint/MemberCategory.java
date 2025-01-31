/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.aot.hint;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * Predefined {@link Member} categories.
 *
 * @author Andy Clement
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public enum MemberCategory {

  /**
   * A category that represents reflective field access on public {@linkplain Field fields}.
   *
   * @see Field#get(Object)
   * @see Field#set(Object, Object)
   * @deprecated in favor of {@link #ACCESS_PUBLIC_FIELDS} with similar semantics.
   */
  @Deprecated(since = "5.0", forRemoval = true)
  PUBLIC_FIELDS,

  /**
   * A category that represents reflective field access on
   * {@linkplain Class#getDeclaredFields() declared fields}: all fields defined by the
   * class but not inherited fields.
   *
   * @see Class#getDeclaredFields()
   * @see Field#get(Object)
   * @see Field#set(Object, Object)
   * @deprecated in favor of {@link #ACCESS_DECLARED_FIELDS} with similar semantics.
   */
  @Deprecated(since = "5.0", forRemoval = true)
  DECLARED_FIELDS,

  /**
   * A category that represents reflective field access on public {@linkplain Field fields}..
   *
   * @see Field#get(Object)
   * @see Field#set(Object, Object)
   * @since 5.0
   */
  ACCESS_PUBLIC_FIELDS,

  /**
   * A category that represents reflective field access on
   * {@linkplain Class#getDeclaredFields() declared fields}: all fields defined by the
   * class but not inherited fields.
   *
   * @see Class#getDeclaredFields()
   * @see Field#get(Object)
   * @see Field#set(Object, Object)
   * @since 5.0
   */
  ACCESS_DECLARED_FIELDS,

  /**
   * A category that defines public {@linkplain Constructor constructors} can
   * be introspected but not invoked.
   *
   * @see Class#getConstructors()
   * @see ExecutableMode#INTROSPECT
   * @deprecated with no replacement since introspection is included
   * when {@link ReflectionHints#registerType(Class, MemberCategory...) adding a reflection hint for a type}.
   */
  @Deprecated(since = "5.0", forRemoval = true)
  INTROSPECT_PUBLIC_CONSTRUCTORS,

  /**
   * A category that defines {@linkplain Class#getDeclaredConstructors() all
   * constructors} can be introspected but not invoked.
   *
   * @see Class#getDeclaredConstructors()
   * @see ExecutableMode#INTROSPECT
   * @deprecated with no replacement since introspection is included
   * when {@link ReflectionHints#registerType(Class, MemberCategory...) adding a reflection hint for a type}.
   */
  @Deprecated(since = "5.0", forRemoval = true)
  INTROSPECT_DECLARED_CONSTRUCTORS,

  /**
   * A category that defines public {@linkplain Constructor constructors} can
   * be invoked.
   *
   * @see Class#getConstructors()
   * @see ExecutableMode#INVOKE
   */
  INVOKE_PUBLIC_CONSTRUCTORS,

  /**
   * A category that defines {@linkplain Class#getDeclaredConstructors() all
   * constructors} can be invoked.
   *
   * @see Class#getDeclaredConstructors()
   * @see ExecutableMode#INVOKE
   */
  INVOKE_DECLARED_CONSTRUCTORS,

  /**
   * A category that defines public {@linkplain Method methods}, including
   * inherited ones, can be introspected but not invoked.
   *
   * @see Class#getMethods()
   * @see ExecutableMode#INTROSPECT
   * @deprecated with no replacement since introspection is added by default
   * when {@link ReflectionHints#registerType(Class, MemberCategory...) adding a reflection hint for a type}.
   */
  @Deprecated(since = "5.0", forRemoval = true)
  INTROSPECT_PUBLIC_METHODS,

  /**
   * A category that defines {@linkplain Class#getDeclaredMethods() all
   * methods}, excluding inherited ones, can be introspected but not invoked.
   *
   * @see Class#getDeclaredMethods()
   * @see ExecutableMode#INTROSPECT
   * @deprecated with no replacement since introspection is added by default
   * when {@link ReflectionHints#registerType(Class, MemberCategory...) adding a reflection hint for a type}.
   */
  @Deprecated(since = "5.0", forRemoval = true)
  INTROSPECT_DECLARED_METHODS,

  /**
   * A category that defines public {@linkplain Method methods}, including
   * inherited ones, can be invoked.
   *
   * @see Class#getMethods()
   * @see ExecutableMode#INVOKE
   */
  INVOKE_PUBLIC_METHODS,

  /**
   * A category that defines {@linkplain Class#getDeclaredMethods() all
   * methods}, excluding inherited ones, can be invoked.
   *
   * @see Class#getDeclaredMethods()
   * @see ExecutableMode#INVOKE
   */
  INVOKE_DECLARED_METHODS,

  /**
   * A category that represents public {@linkplain Class#getClasses() inner
   * classes}.
   * <p>Contrary to other categories, this does not register any particular
   * reflection for inner classes but rather makes sure they are available
   * via a call to {@link Class#getClasses}.
   *
   * @deprecated with no replacement since introspection is included
   * when {@link ReflectionHints#registerType(Class, MemberCategory...) adding a reflection hint for a type}.
   */
  @Deprecated(since = "5.0", forRemoval = true)
  PUBLIC_CLASSES,

  /**
   * A category that represents all {@linkplain Class#getDeclaredClasses()
   * inner classes}.
   * <p>Contrary to other categories, this does not register any particular
   * reflection for inner classes but rather makes sure they are available
   * via a call to {@link Class#getDeclaredClasses}.
   *
   * @deprecated with no replacement since introspection is included
   * when {@link ReflectionHints#registerType(Class, MemberCategory...) adding a reflection hint for a type}.
   */
  @Deprecated(since = "5.0", forRemoval = true)
  DECLARED_CLASSES,

  /**
   * A category that represents the need for
   * {@link sun.misc.Unsafe#allocateInstance(Class) unsafe allocation}
   * for this type.
   *
   */
  UNSAFE_ALLOCATED

}
