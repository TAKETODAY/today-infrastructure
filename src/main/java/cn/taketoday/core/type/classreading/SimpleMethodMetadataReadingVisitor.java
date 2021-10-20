/*
 * Copyright 2002-2021 the original author or authors.
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

package cn.taketoday.core.type.classreading;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.bytecode.AnnotationVisitor;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.lang.Nullable;

/**
 * ASM method visitor that creates {@link SimpleMethodMetadata}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 */
final class SimpleMethodMetadataReadingVisitor extends MethodVisitor {

  @Nullable
  private final ClassLoader classLoader;

  private final String declaringClassName;

  private final int access;

  private final String methodName;

  private final String descriptor;

  private final List<MergedAnnotation<?>> annotations = new ArrayList<>(4);

  private final Consumer<SimpleMethodMetadata> consumer;

  @Nullable
  private Source source;

  SimpleMethodMetadataReadingVisitor(
          @Nullable ClassLoader classLoader, String declaringClassName,
          int access, String methodName, String descriptor, Consumer<SimpleMethodMetadata> consumer) {

    this.classLoader = classLoader;
    this.declaringClassName = declaringClassName;
    this.access = access;
    this.methodName = methodName;
    this.descriptor = descriptor;
    this.consumer = consumer;
  }

  @Override
  @Nullable
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    return MergedAnnotationReadingVisitor.get(this.classLoader, getSource(),
                                              descriptor, visible, this.annotations::add);
  }

  @Override
  public void visitEnd() {
    if (!this.annotations.isEmpty()) {
      String returnTypeName = Type.forReturnType(this.descriptor).getClassName();
      MergedAnnotations annotations = MergedAnnotations.of(this.annotations);
      SimpleMethodMetadata metadata = new SimpleMethodMetadata(
              this.methodName, this.access, this.declaringClassName,
              returnTypeName, getSource(), annotations);
      this.consumer.accept(metadata);
    }
  }

  private Object getSource() {
    Source source = this.source;
    if (source == null) {
      source = new Source(this.declaringClassName, this.methodName, this.descriptor);
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
      return (this.declaringClassName.equals(otherSource.declaringClassName) &&
              this.methodName.equals(otherSource.methodName) && this.descriptor.equals(otherSource.descriptor));
    }

    @Override
    public String toString() {
      String value = this.toStringValue;
      if (value == null) {
        StringBuilder builder = new StringBuilder();
        builder.append(this.declaringClassName);
        builder.append('.');
        builder.append(this.methodName);
        Type[] argumentTypes = Type.getArgumentTypes(this.descriptor);
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
