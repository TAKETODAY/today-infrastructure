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

package cn.taketoday.core.type.classreading;

import java.util.ArrayList;
import java.util.function.Consumer;

import cn.taketoday.bytecode.AnnotationVisitor;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Type;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Nullable;

/**
 * ASM method visitor that creates {@link SimpleMethodMetadata}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class SimpleMethodMetadataReadingVisitor extends MethodVisitor {

  @Nullable
  private final ClassLoader classLoader;

  private final String declaringClassName;

  private final int access;

  @Nullable
  private ArrayList<MergedAnnotation<?>> annotations;

  private final Consumer<SimpleMethodMetadata> consumer;

  @Nullable
  private Source source;

  private final String methodName;

  private final String descriptor;

  SimpleMethodMetadataReadingVisitor(
          @Nullable ClassLoader classLoader, String declaringClassName,
          int access, String methodName, String descriptor, Consumer<SimpleMethodMetadata> consumer) {

    this.access = access;
    this.consumer = consumer;
    this.classLoader = classLoader;
    this.methodName = methodName;
    this.descriptor = descriptor;
    this.declaringClassName = declaringClassName;
  }

  @Override
  @Nullable
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    if (annotations == null) {
      this.annotations = new ArrayList<>(4);
    }
    return MergedAnnotationReadingVisitor.get(
            this.classLoader, getSource(), descriptor, visible, this.annotations::add);
  }

  @Override
  public void visitEnd() {
    String returnTypeName = Type.forReturnType(descriptor).getClassName();
    MergedAnnotations annotations = MergedAnnotations.valueOf(this.annotations);
    SimpleMethodMetadata metadata = new SimpleMethodMetadata(methodName, access,
            declaringClassName, returnTypeName, getSource(), annotations);
    consumer.accept(metadata);
  }

  private Object getSource() {
    Source source = this.source;
    if (source == null) {
      source = new Source(declaringClassName, methodName, descriptor);
      this.source = source;
    }
    return source;
  }

  /**
   * {@link MergedAnnotation} source.
   */
  static final class Source {

    private final String declaringClassName;

    private final String methodName;

    private final String descriptor;

    @Nullable
    private String toStringValue;

    Source(String declaringClassName, String methodName, String descriptor) {
      this.declaringClassName = declaringClassName;
      this.methodName = methodName;
      this.descriptor = descriptor;
    }

    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + this.declaringClassName.hashCode();
      result = 31 * result + this.methodName.hashCode();
      result = 31 * result + this.descriptor.hashCode();
      return result;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      Source otherSource = (Source) other;
      return (this.declaringClassName.equals(otherSource.declaringClassName)
              && this.methodName.equals(otherSource.methodName)
              && this.descriptor.equals(otherSource.descriptor));
    }

    @Override
    public String toString() {
      String value = this.toStringValue;
      if (value == null) {
        StringBuilder builder = new StringBuilder();
        builder.append(this.declaringClassName);
        builder.append('.');
        builder.append(this.methodName);
        Type[] argumentTypes = Type.forArgumentTypes(this.descriptor);
        builder.append('(');
        for (int i = 0; i < argumentTypes.length; i++) {
          if (i != 0) {
            builder.append(',');
          }
          builder.append(argumentTypes[i].getClassName());
        }
        builder.append(')');
        value = builder.toString();
        this.toStringValue = value;
      }
      return value;
    }
  }

}
