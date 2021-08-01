/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.asm.tree;

import java.util.ArrayList;

import cn.taketoday.asm.AnnotationVisitor;

/**
 * A node that represents an annotation.
 *
 * @author Eric Bruneton
 */
public class AnnotationNode extends AnnotationVisitor {

  /** The class descriptor of the annotation class. */
  public String desc;

  /**
   * The name value pairs of this annotation. Each name value pair is stored as two consecutive
   * elements in the list. The name is a {@link String}, and the value may be a {@link Byte}, {@link
   * Boolean}, {@link Character}, {@link Short}, {@link Integer}, {@link Long}, {@link Float},
   * {@link Double}, {@link String} or {@link cn.taketoday.asm.Type}, or a two elements String
   * array (for enumeration values), an {@link AnnotationNode}, or a {@link ArrayList} of values of one
   * of the preceding types. The list may be {@literal null} if there is no name value pair.
   */
  public ArrayList<Object> values;

  /**
   * Constructs a new {@link AnnotationNode}
   *
   * @param descriptor
   *         the class descriptor of the annotation class.
   */
  public AnnotationNode(final String descriptor) {
    this.desc = descriptor;
  }

  /**
   * Constructs a new {@link AnnotationNode} to visit an array value.
   *
   * @param values
   *         where the visited values must be stored.
   */
  AnnotationNode(final ArrayList<Object> values) {
    this.values = values;
  }

  // ------------------------------------------------------------------------
  // Implementation of the AnnotationVisitor abstract class
  // ------------------------------------------------------------------------

  @Override
  public void visit(final String name, final Object value) {
    ArrayList<Object> values = createIfNecessary();
    if (this.desc != null) {
      values.add(name);
    }
    if (value instanceof byte[]) {
      values.add(Util.asArrayList((byte[]) value));
    }
    else if (value instanceof boolean[]) {
      values.add(Util.asArrayList((boolean[]) value));
    }
    else if (value instanceof short[]) {
      values.add(Util.asArrayList((short[]) value));
    }
    else if (value instanceof char[]) {
      values.add(Util.asArrayList((char[]) value));
    }
    else if (value instanceof int[]) {
      values.add(Util.asArrayList((int[]) value));
    }
    else if (value instanceof long[]) {
      values.add(Util.asArrayList((long[]) value));
    }
    else if (value instanceof float[]) {
      values.add(Util.asArrayList((float[]) value));
    }
    else if (value instanceof double[]) {
      values.add(Util.asArrayList((double[]) value));
    }
    else {
      values.add(value);
    }
  }

  @Override
  public void visitEnum(final String name, final String descriptor, final String value) {
    ArrayList<Object> values = createIfNecessary();
    if (this.desc != null) {
      values.add(name);
    }
    values.add(new String[] { descriptor, value });
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
    ArrayList<Object> values = createIfNecessary();
    if (this.desc != null) {
      values.add(name);
    }
    AnnotationNode annotation = new AnnotationNode(descriptor);
    values.add(annotation);
    return annotation;
  }

  private ArrayList<Object> createIfNecessary() {
    ArrayList<Object> values = this.values;
    if (values == null) {
      values = new ArrayList<>(this.desc != null ? 2 : 1);
      this.values = values;
    }
    return values;
  }

  @Override
  public AnnotationVisitor visitArray(final String name) {
    ArrayList<Object> values = createIfNecessary();
    if (this.desc != null) {
      values.add(name);
    }
    ArrayList<Object> array = new ArrayList<>();
    values.add(array);
    return new AnnotationNode(array);
  }

  @Override
  public void visitEnd() {
    // Nothing to do.
    ArrayList<Object> values = this.values;
    if (values != null) {
      values.trimToSize();
    }
  }

  // ------------------------------------------------------------------------
  // Accept methods
  // ------------------------------------------------------------------------

  /**
   * Makes the given visitor visit this annotation.
   *
   * @param annotationVisitor
   *         an annotation visitor. Maybe {@literal null}.
   */
  public void accept(final AnnotationVisitor annotationVisitor) {
    if (annotationVisitor != null) {
      final ArrayList<Object> values = this.values;
      if (values != null) {
        for (int i = 0, n = values.size(); i < n; i += 2) {
          String name = (String) values.get(i);
          Object value = values.get(i + 1);
          accept(annotationVisitor, name, value);
        }
      }
      annotationVisitor.visitEnd();
    }
  }

  /**
   * Makes the given visitor visit a given annotation value.
   *
   * @param annotationVisitor
   *         an annotation visitor. Maybe {@literal null}.
   * @param name
   *         the value name.
   * @param value
   *         the actual value.
   */
  static void accept(
          final AnnotationVisitor annotationVisitor, final String name, final Object value) {
    if (annotationVisitor != null) {
      if (value instanceof String[]) {
        String[] typeValue = (String[]) value;
        annotationVisitor.visitEnum(name, typeValue[0], typeValue[1]);
      }
      else if (value instanceof AnnotationNode) {
        AnnotationNode annotationValue = (AnnotationNode) value;
        annotationValue.accept(annotationVisitor.visitAnnotation(name, annotationValue.desc));
      }
      else if (value instanceof ArrayList) {
        AnnotationVisitor arrayAnnotationVisitor = annotationVisitor.visitArray(name);
        if (arrayAnnotationVisitor != null) {
          ArrayList<?> arrayValue = (ArrayList<?>) value;
          for (Object o : arrayValue) {
            accept(arrayAnnotationVisitor, null, o);
          }
          arrayAnnotationVisitor.visitEnd();
        }
      }
      else {
        annotationVisitor.visit(name, value);
      }
    }
  }
}
