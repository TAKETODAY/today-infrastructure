/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.core.type;

/**
 * Interface that defines abstract access to the annotations of a specific
 * method, in a form that does not require that method's class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @author Mark Pollack
 * @author Chris Beams
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see StandardMethodMetadata
 * @see AnnotationMetadata#getAnnotatedMethods
 * @see AnnotatedTypeMetadata
 * @since 4.0
 */
public interface MethodMetadata extends AnnotatedTypeMetadata {

  /**
   * Get the name of the underlying method.
   */
  String getMethodName();

  /**
   * Get the fully-qualified name of the class that declares the underlying method.
   */
  String getDeclaringClassName();

  /**
   * Get the fully-qualified name of the underlying method's declared return type.
   */
  String getReturnTypeName();

  /**
   * Determine whether the underlying method is effectively abstract:
   * i.e. marked as abstract in a class or declared as a regular,
   * non-default method in an interface.
   */
  boolean isAbstract();

  /**
   * Determine whether the underlying method is declared as 'static'.
   */
  boolean isStatic();

  /**
   * Determine whether the underlying method is marked as 'final'.
   */
  boolean isFinal();

  /**
   * Determine whether the underlying method is overridable,
   * i.e. not marked as static, final, or private.
   */
  boolean isOverridable();

}
