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

package cn.taketoday.bytecode.tree;

import java.util.ArrayList;

import cn.taketoday.bytecode.AnnotationValueHolder;
import cn.taketoday.bytecode.AnnotationVisitor;
import cn.taketoday.bytecode.ClassValueHolder;
import cn.taketoday.bytecode.EnumValueHolder;
import cn.taketoday.bytecode.Type;

/**
 * A node that represents an annotation.
 *
 * @author Eric Bruneton
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class AnnotationNode extends AnnotationVisitor {

  /** The class descriptor of the annotation class. */
  public String desc;

  /**
   * The name value pairs of this annotation. Each name value pair is stored as two consecutive
   * elements in the list. The name is a {@link String}, and the value may be a {@link Byte}, {@link
   * Boolean}, {@link Character}, {@link Short}, {@link Integer}, {@link Long}, {@link Float},
   * {@link Double}, {@link String}, or a{@link EnumValueHolder} (for enumeration values),
   * any {@link AnnotationValueHolder} an {@link AnnotationNode}, or a {@link ArrayList} of values of one
   * of the preceding types. The list may be {@literal null} if there is no name value pair.
   */
  public ArrayList<Object> values;

  /**
   * Constructs a new {@link AnnotationNode}
   *
   * @param descriptor the class descriptor of the annotation class.
   */
  public AnnotationNode(final String descriptor) {
    this.desc = descriptor;
  }

  /**
   * Constructs a new {@link AnnotationNode} to visit an array value.
   *
   * @param values where the visited values must be stored.
   */
  AnnotationNode(final ArrayList<Object> values) {
    this.values = values;
  }

  // ------------------------------------------------------------------------
  // Implementation of the AnnotationVisitor abstract class
  // ------------------------------------------------------------------------

  @Override
  public void visit(final String name, Object value) {
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
      if (value instanceof Type) {
        value = new ClassValueHolder((Type) value);
      }
      values.add(value);
    }
  }

  @Override
  public void visitEnum(final String name, final String descriptor, final String value) {
    ArrayList<Object> values = createIfNecessary();
    if (this.desc != null) {
      values.add(name);
    }
    values.add(new EnumValueHolder(descriptor, value));
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
      values.add(name); // key
    }
    ArrayList<Object> array = new ArrayList<>();
    values.add(array); // array value
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
   * @param visitor an annotation visitor. Maybe {@literal null}.
   */
  public void accept(final AnnotationVisitor visitor) {
    if (visitor != null) {
      final ArrayList<Object> values = this.values;
      if (values != null) {
        for (int i = 0, n = values.size(); i < n; i += 2) {
          String name = (String) values.get(i);
          Object value = values.get(i + 1);
          accept(visitor, name, value);
        }
      }
      visitor.visitEnd();
    }
  }

  /**
   * Makes the given visitor visit a given annotation value.
   *
   * @param visitor an annotation visitor. Maybe {@literal null}.
   * @param name the value name.
   * @param value the actual value.
   */
  static void accept(final AnnotationVisitor visitor, final String name, final Object value) {
    if (visitor != null) {
      if (value instanceof String[] typeValue) {
        visitor.visitEnum(name, typeValue[0], typeValue[1]);
      }
      else if (value instanceof AnnotationNode annotationValue) {
        annotationValue.accept(visitor.visitAnnotation(name, annotationValue.desc));
      }
      else if (value instanceof ArrayList) {
        AnnotationVisitor arrayVisitor = visitor.visitArray(name);
        if (arrayVisitor != null) {
          ArrayList<?> arrayValue = (ArrayList<?>) value;
          for (Object o : arrayValue) {
            accept(arrayVisitor, null, o);
          }
          arrayVisitor.visitEnd();
        }
      }
      else {
        visitor.visit(name, value);
      }
    }
  }
}
