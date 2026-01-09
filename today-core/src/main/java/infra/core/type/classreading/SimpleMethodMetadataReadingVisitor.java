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

import java.util.ArrayList;
import java.util.function.Consumer;

import infra.bytecode.AnnotationVisitor;
import infra.bytecode.MethodVisitor;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;

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

  SimpleMethodMetadataReadingVisitor(@Nullable ClassLoader classLoader, String declaringClassName,
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
      source = new Source(declaringClassName, methodName, access, descriptor);
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

    private final int access;

    private final String descriptor;

    @Nullable
    private String toStringValue;

    Source(String declaringClassName, String methodName, int access, String descriptor) {
      this.declaringClassName = declaringClassName;
      this.methodName = methodName;
      this.access = access;
      this.descriptor = descriptor;
    }

    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + this.declaringClassName.hashCode();
      result = 31 * result + this.methodName.hashCode();
      result = 31 * result + this.access;
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
              this.methodName.equals(otherSource.methodName) &&
              this.access == otherSource.access && this.descriptor.equals(otherSource.descriptor));
    }

    @Override
    public String toString() {
      String value = this.toStringValue;
      if (value == null) {
        StringBuilder builder = new StringBuilder();
        if ((this.access & Opcodes.ACC_PUBLIC) != 0) {
          builder.append("public ");
        }
        if ((this.access & Opcodes.ACC_PROTECTED) != 0) {
          builder.append("protected ");
        }
        if ((this.access & Opcodes.ACC_PRIVATE) != 0) {
          builder.append("private ");
        }
        if ((this.access & Opcodes.ACC_ABSTRACT) != 0) {
          builder.append("abstract ");
        }
        if ((this.access & Opcodes.ACC_STATIC) != 0) {
          builder.append("static ");
        }
        if ((this.access & Opcodes.ACC_FINAL) != 0) {
          builder.append("final ");
        }
        Type returnType = Type.forReturnType(this.descriptor);
        builder.append(returnType.getClassName());
        builder.append(' ');
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
