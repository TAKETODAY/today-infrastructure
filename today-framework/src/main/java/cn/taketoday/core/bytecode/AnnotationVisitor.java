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
package cn.taketoday.core.bytecode;

/**
 * A visitor to visit a Java annotation. The methods of this class must be called in the following
 * order: ( {@code visit} | {@code visitEnum} | {@code visitAnnotation} | {@code visitArray} )*
 * {@code visitEnd}.
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public abstract class AnnotationVisitor {

  /**
   * The annotation visitor to which this visitor must delegate method calls. May
   * be null.
   */
  protected AnnotationVisitor av;

  /**
   * Constructs a new {@link AnnotationVisitor}.
   */
  public AnnotationVisitor() {
    this(null);
  }

  /**
   * Constructs a new {@link AnnotationVisitor}.
   *
   * @param annotationVisitor the annotation visitor to which this visitor must delegate method
   * calls. May be {@literal null}.
   */
  public AnnotationVisitor(final AnnotationVisitor annotationVisitor) {
    this.av = annotationVisitor;
  }

  /**
   * Visits a primitive value of the annotation.
   *
   * @param name the value name.
   * @param value the actual value, whose type must be {@link Byte}, {@link Boolean}, {@link
   * Character}, {@link Short}, {@link Integer} , {@link Long}, {@link Float}, {@link Double},
   * {@link String} or {@link Type} of {@link Type#OBJECT} or {@link Type#ARRAY} sort. This
   * value can also be an array of byte, boolean, short, char, int, long, float or double values
   * (this is equivalent to using {@link #visitArray} and visiting each array element in turn,
   * but is more convenient).
   */
  public void visit(final String name, final Object value) {
    if (av != null) {
      av.visit(name, value);
    }
  }

  /**
   * Visits an enumeration value of the annotation.
   *
   * @param name the value name.
   * @param descriptor the class descriptor of the enumeration class.
   * @param value the actual enumeration value.
   */
  public void visitEnum(final String name, final String descriptor, final String value) {
    if (av != null) {
      av.visitEnum(name, descriptor, value);
    }
  }

  /**
   * Visits a nested annotation value of the annotation.
   *
   * @param name the value name.
   * @param descriptor the class descriptor of the nested annotation class.
   * @return a visitor to visit the actual nested annotation value, or {@literal null} if this
   * visitor is not interested in visiting this nested annotation. <i>The nested annotation
   * value must be fully visited before calling other methods on this annotation visitor</i>.
   */
  public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
    if (av != null) {
      return av.visitAnnotation(name, descriptor);
    }
    return null;
  }

  /**
   * Visits an array value of the annotation. Note that arrays of primitive values (such as byte,
   * boolean, short, char, int, long, float or double) can be passed as value to {@link #visit
   * visit}. This is what {@link ClassReader} does for non empty arrays of primitive values.
   *
   * @param name the value name.
   * @return a visitor to visit the actual array value elements, or {@literal null} if this visitor
   * is not interested in visiting these values. The 'name' parameters passed to the methods of
   * this visitor are ignored. <i>All the array values must be visited before calling other
   * methods on this annotation visitor</i>.
   */
  public AnnotationVisitor visitArray(final String name) {
    if (av != null) {
      return av.visitArray(name);
    }
    return null;
  }

  /** Visits the end of the annotation. */
  public void visitEnd() {
    if (av != null) {
      av.visitEnd();
    }
  }
}
