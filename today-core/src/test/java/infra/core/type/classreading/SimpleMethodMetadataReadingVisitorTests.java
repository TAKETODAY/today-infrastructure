/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.core.type.classreading;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

import infra.bytecode.AnnotationVisitor;
import infra.bytecode.Opcodes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/26 22:02
 */
class SimpleMethodMetadataReadingVisitorTests {

  @Test
  void emptyAnnotationsWhenNoAnnotationsVisited() {
    var consumer = new ArrayList<SimpleMethodMetadata>();
    var visitor = new SimpleMethodMetadataReadingVisitor(
            null, "foo.Bar", Opcodes.ACC_PUBLIC,
            "methodName", "()V", consumer::add);

    visitor.visitEnd();

    assertThat(consumer).hasSize(1);
    SimpleMethodMetadata metadata = consumer.get(0);
    assertThat(metadata.getAnnotations()).isEmpty();
  }

  @Test
  void publicMethodWithNoArgs() {
    var consumer = new ArrayList<SimpleMethodMetadata>();
    var visitor = new SimpleMethodMetadataReadingVisitor(
            null, "foo.Bar", Opcodes.ACC_PUBLIC,
            "methodName", "()V", consumer::add);

    visitor.visitEnd();

    SimpleMethodMetadata metadata = consumer.get(0);
    assertThat(metadata.getMethodName()).isEqualTo("methodName");
    assertThat(metadata.getReturnTypeName()).isEqualTo("void");
  }

  @Test
  void privateStaticMethodWithArguments() {
    var consumer = new ArrayList<SimpleMethodMetadata>();
    var visitor = new SimpleMethodMetadataReadingVisitor(
            null, "foo.Bar", Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
            "methodName", "(Ljava/lang/String;I)Z", consumer::add);

    visitor.visitEnd();

    SimpleMethodMetadata metadata = consumer.get(0);
    assertThat(metadata.isPrivate()).isTrue();
    assertThat(metadata.isStatic()).isTrue();
    assertThat(metadata.getReturnTypeName()).isEqualTo("boolean");
  }

  @Test
  void sourceToStringFormatsMethodSignature() {
    var source = new SimpleMethodMetadataReadingVisitor.Source(
            "foo.Bar", "methodName",
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "(Ljava/lang/String;)V");

    assertThat(source.toString())
            .isEqualTo("public static void foo.Bar.methodName(java.lang.String)");
  }

  @Test
  void sourceEquals() {
    var source1 = new SimpleMethodMetadataReadingVisitor.Source(
            "foo.Bar", "methodName", Opcodes.ACC_PUBLIC, "()V");
    var source2 = new SimpleMethodMetadataReadingVisitor.Source(
            "foo.Bar", "methodName", Opcodes.ACC_PUBLIC, "()V");
    var source3 = new SimpleMethodMetadataReadingVisitor.Source(
            "foo.Bar", "otherMethod", Opcodes.ACC_PUBLIC, "()V");

    assertThat(source1)
            .isEqualTo(source1)
            .isEqualTo(source2)
            .isNotEqualTo(source3)
            .isNotEqualTo(null)
            .isNotEqualTo(new Object());
  }

  @Test
  void visitorShouldHandleFinalMethod() {
    var consumer = new ArrayList<SimpleMethodMetadata>();
    var visitor = new SimpleMethodMetadataReadingVisitor(
            null, "foo.Bar", Opcodes.ACC_FINAL,
            "methodName", "()V", consumer::add);

    visitor.visitEnd();

    SimpleMethodMetadata metadata = consumer.get(0);
    assertThat(metadata.isFinal()).isTrue();
  }

  @Test
  void visitorShouldHandleAbstractMethod() {
    var consumer = new ArrayList<SimpleMethodMetadata>();
    var visitor = new SimpleMethodMetadataReadingVisitor(
            null, "foo.Bar", Opcodes.ACC_ABSTRACT,
            "methodName", "()V", consumer::add);

    visitor.visitEnd();

    SimpleMethodMetadata metadata = consumer.get(0);
    assertThat(metadata.isAbstract()).isTrue();
  }

  @Test
  void methodWithMultipleParameters() {
    var consumer = new ArrayList<SimpleMethodMetadata>();
    var visitor = new SimpleMethodMetadataReadingVisitor(
            null, "foo.Bar", Opcodes.ACC_PUBLIC,
            "methodName", "(Ljava/lang/String;ILjava/lang/Object;)V", consumer::add);

    visitor.visitEnd();

    SimpleMethodMetadata metadata = consumer.get(0);
    assertThat(metadata.getReturnTypeName()).isEqualTo("void");
  }

  @Test
  void sourceToStringWithMultipleModifiers() {
    var source = new SimpleMethodMetadataReadingVisitor.Source(
            "foo.Bar", "methodName",
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
            "()V");

    assertThat(source.toString())
            .isEqualTo("public static final void foo.Bar.methodName()");
  }

  @Test
  void sourceHashCodeConsistency() {
    var source1 = new SimpleMethodMetadataReadingVisitor.Source(
            "foo.Bar", "methodName", Opcodes.ACC_PUBLIC, "()V");
    var source2 = new SimpleMethodMetadataReadingVisitor.Source(
            "foo.Bar", "methodName", Opcodes.ACC_PUBLIC, "()V");

    assertThat(source1.hashCode()).isEqualTo(source2.hashCode());
  }

  @Test
  @Disabled
  void visitAnnotationCreatesAnnotationVisitor() {
    var consumer = new ArrayList<SimpleMethodMetadata>();
    var visitor = new SimpleMethodMetadataReadingVisitor(
            getClass().getClassLoader(), "foo.Bar", Opcodes.ACC_PUBLIC,
            "methodName", "()V", consumer::add);

    AnnotationVisitor annotationVisitor = visitor.visitAnnotation(
            "Ljavax/annotation/Nullable;", true);

    assertThat(annotationVisitor).isNotNull();
    visitor.visitEnd();
    SimpleMethodMetadata metadata = consumer.get(0);
    assertThat(metadata.getAnnotations()).isNotEmpty();
  }

  @Test
  void methodWithInnerClassReturnType() {
    var consumer = new ArrayList<SimpleMethodMetadata>();
    var visitor = new SimpleMethodMetadataReadingVisitor(
            null, "foo.Bar", Opcodes.ACC_PUBLIC,
            "methodName", "()Lfoo/Bar$Inner;", consumer::add);

    visitor.visitEnd();

    SimpleMethodMetadata metadata = consumer.get(0);
    assertThat(metadata.getReturnTypeName()).isEqualTo("foo.Bar$Inner");
  }

  @Test
  void sourceToStringWithArrayParameters() {
    var source = new SimpleMethodMetadataReadingVisitor.Source(
            "foo.Bar", "methodName",
            Opcodes.ACC_PUBLIC,
            "([Ljava/lang/String;[I)V");

    assertThat(source.toString())
            .isEqualTo("public void foo.Bar.methodName(java.lang.String[],int[])");
  }

  @Test
  void methodWithMultiDimensionalArrays() {
    var consumer = new ArrayList<SimpleMethodMetadata>();
    var visitor = new SimpleMethodMetadataReadingVisitor(
            null, "foo.Bar", Opcodes.ACC_PUBLIC,
            "methodName", "([[Ljava/lang/String;)[[I", consumer::add);

    visitor.visitEnd();

    SimpleMethodMetadata metadata = consumer.get(0);
    assertThat(metadata.getReturnTypeName()).isEqualTo("int[][]");
  }

  @Test
  void sourceEqualsWithDifferentAccess() {
    var source1 = new SimpleMethodMetadataReadingVisitor.Source(
            "foo.Bar", "methodName", Opcodes.ACC_PUBLIC, "()V");
    var source2 = new SimpleMethodMetadataReadingVisitor.Source(
            "foo.Bar", "methodName", Opcodes.ACC_PRIVATE, "()V");

    assertThat(source1).isNotEqualTo(source2);
  }

  @Test
  void methodWithGenericReturnType() {
    var consumer = new ArrayList<SimpleMethodMetadata>();
    var visitor = new SimpleMethodMetadataReadingVisitor(
            null, "foo.Bar", Opcodes.ACC_PUBLIC,
            "methodName", "()Ljava/util/List;", consumer::add);

    visitor.visitEnd();

    SimpleMethodMetadata metadata = consumer.get(0);
    assertThat(metadata.getReturnTypeName()).isEqualTo("java.util.List");
  }

  @Test
  void methodWithThrowsClause() {
    var consumer = new ArrayList<SimpleMethodMetadata>();
    var visitor = new SimpleMethodMetadataReadingVisitor(
            null, "foo.Bar", Opcodes.ACC_PUBLIC,
            "methodName", "()V", consumer::add);

    visitor.visitEnd();

    SimpleMethodMetadata metadata = consumer.get(0);
    assertThat(Modifier.isPublic(metadata.access)).isTrue();
    assertThat(metadata.isOverridable()).isTrue();
  }

  @Test
  void methodWithGenericParameter() {
    var consumer = new ArrayList<SimpleMethodMetadata>();
    var visitor = new SimpleMethodMetadataReadingVisitor(
            null, "foo.Bar", Opcodes.ACC_PUBLIC,
            "methodName", "(Ljava/util/List;)V", consumer::add);

    visitor.visitEnd();

    SimpleMethodMetadata metadata = consumer.get(0);
    assertThat(metadata.getReturnTypeName()).isEqualTo("void");
  }

  @Test
  void synchronizedMethod() {
    var consumer = new ArrayList<SimpleMethodMetadata>();
    var visitor = new SimpleMethodMetadataReadingVisitor(
            null, "foo.Bar", Opcodes.ACC_SYNCHRONIZED,
            "methodName", "()V", consumer::add);

    visitor.visitEnd();

    SimpleMethodMetadata metadata = consumer.get(0);
    assertThat(Modifier.isSynchronized(metadata.access)).isTrue();
  }

  @Test
  void nativeMethod() {
    var consumer = new ArrayList<SimpleMethodMetadata>();
    var visitor = new SimpleMethodMetadataReadingVisitor(
            null, "foo.Bar", Opcodes.ACC_NATIVE,
            "methodName", "()V", consumer::add);

    visitor.visitEnd();

    SimpleMethodMetadata metadata = consumer.get(0);
    assertThat(Modifier.isNative(metadata.access)).isTrue();
  }

}
