/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core.type.classreading;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

import infra.bytecode.AnnotationVisitor;
import infra.bytecode.Type;
import infra.core.annotation.AnnotationFilter;
import infra.core.annotation.MergedAnnotation;
import infra.util.ClassUtils;

/**
 * {@link AnnotationVisitor} that can be used to construct a
 * {@link MergedAnnotation}.
 *
 * @param <A> the annotation type
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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

  public MergedAnnotationReadingVisitor(@Nullable ClassLoader classLoader,
          @Nullable Object source, Class<A> annotationType, Consumer<MergedAnnotation<A>> consumer) {

    this.source = source;
    this.consumer = consumer;
    this.classLoader = classLoader;
    this.annotationType = annotationType;
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
    String className = Type.forDescriptor(descriptor).getClassName();
    Class<E> type = ClassUtils.resolveClassName(className, this.classLoader);
    consumer.accept(Enum.valueOf(type, value));
  }

  @Nullable
  private <T extends Annotation> AnnotationVisitor visitAnnotation(
          String descriptor, Consumer<MergedAnnotation<T>> consumer) {

    String className = Type.forDescriptor(descriptor).getClassName();
    if (AnnotationFilter.PLAIN.matches(className)) {
      return null;
    }
    Class<T> type = ClassUtils.resolveClassName(className, this.classLoader);
    return new MergedAnnotationReadingVisitor<>(this.classLoader, this.source, type, consumer);
  }

  @Nullable
  static <A extends Annotation> AnnotationVisitor get(@Nullable ClassLoader classLoader,
          @Nullable Object source, String descriptor, boolean visible, Consumer<MergedAnnotation<A>> consumer) {

    if (!visible) {
      return null;
    }

    String typeName = Type.forDescriptor(descriptor).getClassName();
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
