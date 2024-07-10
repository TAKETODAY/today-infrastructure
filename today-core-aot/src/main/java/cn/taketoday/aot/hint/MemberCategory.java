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

package cn.taketoday.aot.hint;

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
 * @since 4.0
 */
public enum MemberCategory {

  /**
   * A category that represents public {@linkplain Field fields}.
   *
   * @see Class#getFields()
   */
  PUBLIC_FIELDS,

  /**
   * A category that represents {@linkplain Class#getDeclaredFields() declared
   * fields}: all fields defined by the class but not inherited fields.
   *
   * @see Class#getDeclaredFields()
   */
  DECLARED_FIELDS,

  /**
   * A category that defines public {@linkplain Constructor constructors} can
   * be introspected but not invoked.
   *
   * @see Class#getConstructors()
   * @see ExecutableMode#INTROSPECT
   */
  INTROSPECT_PUBLIC_CONSTRUCTORS,

  /**
   * A category that defines {@linkplain Class#getDeclaredConstructors() all
   * constructors} can be introspected but not invoked.
   *
   * @see Class#getDeclaredConstructors()
   * @see ExecutableMode#INTROSPECT
   */
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
   */
  INTROSPECT_PUBLIC_METHODS,

  /**
   * A category that defines {@linkplain Class#getDeclaredMethods() all
   * methods}, excluding inherited ones, can be introspected but not invoked.
   *
   * @see Class#getDeclaredMethods()
   * @see ExecutableMode#INTROSPECT
   */
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
   */
  PUBLIC_CLASSES,

  /**
   * A category that represents all {@linkplain Class#getDeclaredClasses()
   * inner classes}.
   * <p>Contrary to other categories, this does not register any particular
   * reflection for inner classes but rather makes sure they are available
   * via a call to {@link Class#getDeclaredClasses}.
   */
  DECLARED_CLASSES;

}
