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

package cn.taketoday.core.type.classreading;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.core.annotation.AnnotationFilter;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.bytecode.AnnotationVisitor;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * {@link AnnotationVisitor} that can be used to construct a
 * {@link MergedAnnotation}.
 *
 * @param <A> the annotation type
 * @author Phillip Webb
 * @since 4.0
 */
class MergedAnnotationReadingVisitor<A extends Annotation> extends AnnotationVisitor {

  @Nullable
  private final ClassLoader classLoader;

  @Nullable
  private final Object source;

  private final Class<A> annotationType;

  private final Consumer<MergedAnnotation<A>> consumer;

  private final LinkedHashMap<String, Object> attributes = new LinkedHashMap<>(4);

  public MergedAnnotationReadingVisitor
          (@Nullable ClassLoader classLoader, @Nullable Object source,
           Class<A> annotationType, Consumer<MergedAnnotation<A>> consumer) {

    this.classLoader = classLoader;
    this.source = source;
    this.annotationType = annotationType;
    this.consumer = consumer;
  }

  @Override
  public void visit(String name, Object value) {
    if (value instanceof Type) {
      value = ((Type) value).getClassName();
    }
    this.attributes.put(name, value);
  }

  @Override
  public void visitEnum(String name, String descriptor, String value) {
    visitEnum(descriptor, value, enumValue -> this.attributes.put(name, enumValue));
  }

  @Override
  @Nullable
  public AnnotationVisitor visitAnnotation(String name, String descriptor) {
    return visitAnnotation(descriptor, annotation -> this.attributes.put(name, annotation));
  }

  @Override
  public AnnotationVisitor visitArray(String name) {
    return new ArrayVisitor(value -> this.attributes.put(name, value));
  }

  @Override
  public void visitEnd() {
    MergedAnnotation<A> annotation = MergedAnnotation.valueOf(
            this.classLoader, this.source, this.annotationType, this.attributes);
    this.consumer.accept(annotation);
  }

  public <E extends Enum<E>> void visitEnum(String descriptor, String value, Consumer<E> consumer) {
    String className = Type.fromDescriptor(descriptor).getClassName();
    Class<E> type = ClassUtils.resolveClassName(className, this.classLoader);
    consumer.accept(Enum.valueOf(type, value));
  }

  @Nullable
  private <T extends Annotation> AnnotationVisitor visitAnnotation(
          String descriptor, Consumer<MergedAnnotation<T>> consumer) {

    String className = Type.fromDescriptor(descriptor).getClassName();
    if (AnnotationFilter.PLAIN.matches(className)) {
      return null;
    }
    Class<T> type = ClassUtils.resolveClassName(className, this.classLoader);
    return new MergedAnnotationReadingVisitor<>(this.classLoader, this.source, type, consumer);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  static <A extends Annotation> AnnotationVisitor get(
          @Nullable ClassLoader classLoader,
          @Nullable Object source, String descriptor, boolean visible,
          Consumer<MergedAnnotation<A>> consumer) {

    if (!visible) {
      return null;
    }

    String typeName = Type.fromDescriptor(descriptor).getClassName();
    if (AnnotationFilter.PLAIN.matches(typeName)) {
      return null;
    }

    try {
      Class<A> annotationType = ClassUtils.forName(typeName, classLoader);
      return new MergedAnnotationReadingVisitor<>(classLoader, source, annotationType, consumer);
    }
    catch (ClassNotFoundException | LinkageError ex) {
      return null;
    }
  }

  /**
   * {@link AnnotationVisitor} to deal with array attributes.
   */
  private class ArrayVisitor extends AnnotationVisitor {

    private final List<Object> elements = new ArrayList<>();

    private final Consumer<Object[]> consumer;

    ArrayVisitor(Consumer<Object[]> consumer) {
      this.consumer = consumer;
    }

    @Override
    public void visit(String name, Object value) {
      if (value instanceof Type) {
        value = ((Type) value).getClassName();
      }
      this.elements.add(value);
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
      MergedAnnotationReadingVisitor.this.visitEnum(descriptor, value, this.elements::add);
    }

    @Override
    @Nullable
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
      return MergedAnnotationReadingVisitor.this.visitAnnotation(descriptor, this.elements::add);
    }

    @Override
    public void visitEnd() {
      Class<?> componentType = getComponentType();
      Object[] array = (Object[]) Array.newInstance(componentType, this.elements.size());
      this.consumer.accept(this.elements.toArray(array));
    }

    private Class<?> getComponentType() {
      if (this.elements.isEmpty()) {
        return Object.class;
      }
      Object firstElement = this.elements.get(0);
      if (firstElement instanceof Enum) {
        return ((Enum<?>) firstElement).getDeclaringClass();
      }
      return firstElement.getClass();
    }
  }

}
