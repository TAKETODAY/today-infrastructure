/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.bytecode;

/**
 * A visitor to visit a Java field. The methods of this class must be called in the following order:
 * ( {@code visitAnnotation} | {@code visitTypeAnnotation} | {@code visitAttribute} )* {@code
 * visitEnd}.
 * <p>
 * default is empty implementation
 * </p>
 *
 * @author Eric Bruneton
 */
public class FieldVisitor {

  /**
   * The field visitor to which this visitor must delegate method calls. May be
   * null.
   */
  protected FieldVisitor fv;

  /**
   * Constructs a new {@link FieldVisitor}.
   */
  public FieldVisitor() {
    this(null);
  }

  /**
   * Constructs a new {@link FieldVisitor}.
   *
   * @param fieldVisitor the field visitor to which this visitor must delegate method
   * calls. May be null.
   */
  public FieldVisitor(final FieldVisitor fieldVisitor) {
    this.fv = fieldVisitor;
  }

  /**
   * Visits an annotation of the field.
   *
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
   * interested in visiting this annotation.
   */
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (fv != null) {
      return fv.visitAnnotation(descriptor, visible);
    }
    return null;
  }

  /**
   * Visits an annotation on the type of the field.
   *
   * @param typeRef a reference to the annotated type. The sort of this type reference must be
   * {@link TypeReference#FIELD}. See {@link TypeReference}.
   * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
   * static inner type within 'typeRef'. May be {@literal null} if the annotation targets
   * 'typeRef' as a whole.
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
   * interested in visiting this annotation.
   */
  public AnnotationVisitor visitTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (fv != null) {
      return fv.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  /**
   * Visits a non standard attribute of the field.
   *
   * @param attribute an attribute.
   */
  public void visitAttribute(final Attribute attribute) {
    if (fv != null) {
      fv.visitAttribute(attribute);
    }
  }

  /**
   * Visits the end of the field. This method, which is the last one to be called, is used to inform
   * the visitor that all the annotations and attributes of the field have been visited.
   */
  public void visitEnd() {
    if (fv != null) {
      fv.visitEnd();
    }
  }
}
