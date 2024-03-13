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

import java.util.Set;

import cn.taketoday.lang.Nullable;

/**
 * Interface that defines abstract metadata of a specific class,
 * in a form that does not require that class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see StandardClassMetadata
 * @see cn.taketoday.core.type.classreading.MetadataReader#getClassMetadata()
 * @see AnnotationMetadata
 * @since 4.0
 */
public interface ClassMetadata {

  /**
   * Return the name of the underlying class.
   */
  String getClassName();

  /**
   * Return whether the underlying class represents an interface.
   */
  boolean isInterface();

  /**
   * Return whether the underlying class represents an annotation.
   */
  boolean isAnnotation();

  /**
   * Return whether the underlying class is marked as abstract.
   */
  boolean isAbstract();

  /**
   * Return whether the underlying class represents a concrete class,
   * i.e. neither an interface nor an abstract class.
   */
  default boolean isConcrete() {
    return !(isInterface() || isAbstract());
  }

  /**
   * Return whether the underlying class is marked as 'final'.
   */
  boolean isFinal();

  /**
   * Determine whether the underlying class is independent, i.e. whether
   * it is a top-level class or a nested class (static inner class) that
   * can be constructed independently from an enclosing class.
   */
  boolean isIndependent();

  /**
   * Return whether the underlying class is declared within an enclosing
   * class (i.e. the underlying class is an inner/nested class or a
   * local class within a method).
   * <p>If this method returns {@code false}, then the underlying
   * class is a top-level class.
   */
  default boolean hasEnclosingClass() {
    return (getEnclosingClassName() != null);
  }

  /**
   * Return the name of the enclosing class of the underlying class,
   * or {@code null} if the underlying class is a top-level class.
   */
  @Nullable
  String getEnclosingClassName();

  /**
   * Return whether the underlying class has a super class.
   */
  default boolean hasSuperClass() {
    return (getSuperClassName() != null);
  }

  /**
   * Return the name of the super class of the underlying class,
   * or {@code null} if there is no super class defined.
   */
  @Nullable
  String getSuperClassName();

  /**
   * Return the names of all interfaces that the underlying class
   * implements, or an empty array if there are none.
   */
  String[] getInterfaceNames();

  /**
   * Return the names of all classes declared as members of the class represented by
   * this ClassMetadata object. This includes public, protected, default (package)
   * access, and private classes and interfaces declared by the class, but excludes
   * inherited classes and interfaces. An empty array is returned if no member classes
   * or interfaces exist.
   */
  String[] getMemberClassNames();

  int getModifiers();

  /**
   * Retrieve the method metadata for all user-declared methods on the
   * underlying class, preserving declaration order as far as possible.
   *
   * @return a set of {@link MethodMetadata}
   */
  Set<MethodMetadata> getDeclaredMethods();
}
