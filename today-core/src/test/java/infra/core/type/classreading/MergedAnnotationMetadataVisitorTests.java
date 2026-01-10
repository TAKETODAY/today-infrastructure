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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import infra.bytecode.AnnotationVisitor;
import infra.bytecode.ClassReader;
import infra.bytecode.ClassVisitor;
import infra.core.annotation.MergedAnnotation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MergedAnnotationReadingVisitor}.
 *
 * @author Phillip Webb
 */
class MergedAnnotationMetadataVisitorTests {

  private MergedAnnotation<?> annotation;

  @Test
  void visitWhenHasSimpleTypesCreatesAnnotation() {
    loadFrom(WithSimpleTypesAnnotation.class);
    assertThat(this.annotation.getType()).isEqualTo(SimpleTypesAnnotation.class);
    assertThat(this.annotation.getValue("stringValue")).isEqualTo("string");
    assertThat(this.annotation.getValue("byteValue")).isEqualTo((byte) 1);
    assertThat(this.annotation.getValue("shortValue")).isEqualTo((short) 2);
    assertThat(this.annotation.getValue("intValue")).isEqualTo(3);
    assertThat(this.annotation.getValue("longValue")).isEqualTo(4L);
    assertThat(this.annotation.getValue("booleanValue")).isEqualTo(true);
    assertThat(this.annotation.getValue("charValue")).isEqualTo('c');
    assertThat(this.annotation.getValue("doubleValue")).isEqualTo(5.0);
    assertThat(this.annotation.getValue("floatValue")).isEqualTo(6.0f);
  }

  @Test
  void visitWhenHasSimpleArrayTypesCreatesAnnotation() {
    loadFrom(WithSimpleArrayTypesAnnotation.class);
    assertThat(this.annotation.getType()).isEqualTo(SimpleArrayTypesAnnotation.class);
    assertThat(this.annotation.getValue("stringValue")).isEqualTo(
            new String[] { "string" });
    assertThat(this.annotation.getValue("byteValue")).isEqualTo(new byte[] { 1 });
    assertThat(this.annotation.getValue("shortValue")).isEqualTo(new short[] { 2 });
    assertThat(this.annotation.getValue("intValue")).isEqualTo(new int[] { 3 });
    assertThat(this.annotation.getValue("longValue")).isEqualTo(new long[] { 4 });
    assertThat(this.annotation.getValue("booleanValue")).isEqualTo(
            new boolean[] { true });
    assertThat(this.annotation.getValue("charValue")).isEqualTo(new char[] { 'c' });
    assertThat(this.annotation.getValue("doubleValue")).isEqualTo(
            new double[] { 5.0 });
    assertThat(this.annotation.getValue("floatValue")).isEqualTo(new float[] { 6.0f });
  }

  @Test
  void visitWhenHasEmptySimpleArrayTypesCreatesAnnotation() {
    loadFrom(WithSimpleEmptyArrayTypesAnnotation.class);
    assertThat(this.annotation.getType()).isEqualTo(SimpleArrayTypesAnnotation.class);
    assertThat(this.annotation.getValue("stringValue")).isEqualTo(new String[] {});
    assertThat(this.annotation.getValue("byteValue")).isEqualTo(new byte[] {});
    assertThat(this.annotation.getValue("shortValue")).isEqualTo(new short[] {});
    assertThat(this.annotation.getValue("intValue")).isEqualTo(new int[] {});
    assertThat(this.annotation.getValue("longValue")).isEqualTo(new long[] {});
    assertThat(this.annotation.getValue("booleanValue")).isEqualTo(new boolean[] {});
    assertThat(this.annotation.getValue("charValue")).isEqualTo(new char[] {});
    assertThat(this.annotation.getValue("doubleValue")).isEqualTo(new double[] {});
    assertThat(this.annotation.getValue("floatValue")).isEqualTo(new float[] {});
  }

  @Test
  void visitWhenHasEnumAttributesCreatesAnnotation() {
    loadFrom(WithEnumAnnotation.class);
    assertThat(this.annotation.getType()).isEqualTo(EnumAnnotation.class);
    assertThat(this.annotation.getValue("enumValue")).isEqualTo(ExampleEnum.ONE);
    assertThat(this.annotation.getValue("enumArrayValue")).isEqualTo(
            new ExampleEnum[] { ExampleEnum.ONE, ExampleEnum.TWO });
  }

  @Test
  void visitWhenHasAnnotationAttributesCreatesAnnotation() {
    loadFrom(WithAnnotationAnnotation.class);
    assertThat(this.annotation.getType()).isEqualTo(AnnotationAnnotation.class);
    MergedAnnotation<NestedAnnotation> value = this.annotation.getAnnotation(
            "annotationValue", NestedAnnotation.class);
    assertThat(value.isPresent()).isTrue();
    assertThat(value.getString(MergedAnnotation.VALUE)).isEqualTo("a");
    MergedAnnotation<NestedAnnotation>[] arrayValue = this.annotation.getAnnotationArray(
            "annotationArrayValue", NestedAnnotation.class);
    assertThat(arrayValue).hasSize(2);
    assertThat(arrayValue[0].getString(MergedAnnotation.VALUE)).isEqualTo("b");
    assertThat(arrayValue[1].getString(MergedAnnotation.VALUE)).isEqualTo("c");
  }

  @Test
  void visitWhenHasClassAttributesCreatesAnnotation() {
    loadFrom(WithClassAnnotation.class);
    assertThat(this.annotation.getType()).isEqualTo(ClassAnnotation.class);
    assertThat(this.annotation.getString("classValue")).isEqualTo(InputStream.class.getName());
    assertThat(this.annotation.getClass("classValue")).isEqualTo(InputStream.class);
    assertThat(this.annotation.getValue("classValue")).isEqualTo(InputStream.class);
    assertThat(this.annotation.getStringArray("classArrayValue")).containsExactly(OutputStream.class.getName());
    assertThat(this.annotation.getValue("classArrayValue")).isEqualTo(new Class<?>[] { OutputStream.class });
  }

  private void loadFrom(Class<?> type) {
    ClassVisitor visitor = new ClassVisitor() {

      @Override
      public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return MergedAnnotationReadingVisitor.get(getClass().getClassLoader(),
                null, descriptor, visible,
                annotation -> MergedAnnotationMetadataVisitorTests.this.annotation = annotation);
      }

    };
    try {
      new ClassReader(type.getName()).accept(visitor, ClassReader.SKIP_DEBUG
              | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
    }
    catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @SimpleTypesAnnotation(stringValue = "string", byteValue = 1, shortValue = 2, intValue = 3, longValue = 4, booleanValue = true, charValue = 'c', doubleValue = 5.0, floatValue = 6.0f)
  static class WithSimpleTypesAnnotation {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface SimpleTypesAnnotation {

    String stringValue();

    byte byteValue();

    short shortValue();

    int intValue();

    long longValue();

    boolean booleanValue();

    char charValue();

    double doubleValue();

    float floatValue();

  }

  @SimpleArrayTypesAnnotation(stringValue = "string", byteValue = 1, shortValue = 2, intValue = 3, longValue = 4, booleanValue = true, charValue = 'c', doubleValue = 5.0, floatValue = 6.0f)
  static class WithSimpleArrayTypesAnnotation {

  }

  @SimpleArrayTypesAnnotation(stringValue = {}, byteValue = {}, shortValue = {}, intValue = {}, longValue = {}, booleanValue = {}, charValue = {}, doubleValue = {}, floatValue = {})
  static class WithSimpleEmptyArrayTypesAnnotation {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface SimpleArrayTypesAnnotation {

    String[] stringValue();

    byte[] byteValue();

    short[] shortValue();

    int[] intValue();

    long[] longValue();

    boolean[] booleanValue();

    char[] charValue();

    double[] doubleValue();

    float[] floatValue();

  }

  @EnumAnnotation(enumValue = ExampleEnum.ONE, enumArrayValue = { ExampleEnum.ONE,
          ExampleEnum.TWO })
  static class WithEnumAnnotation {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface EnumAnnotation {

    ExampleEnum enumValue();

    ExampleEnum[] enumArrayValue();

  }

  enum ExampleEnum {
    ONE, TWO, THREE
  }

  @AnnotationAnnotation(annotationValue = @NestedAnnotation("a"), annotationArrayValue = {
          @NestedAnnotation("b"), @NestedAnnotation("c") })
  static class WithAnnotationAnnotation {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface AnnotationAnnotation {

    NestedAnnotation annotationValue();

    NestedAnnotation[] annotationArrayValue();

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface NestedAnnotation {

    String value() default "";

  }

  @ClassAnnotation(classValue = InputStream.class, classArrayValue = OutputStream.class)
  static class WithClassAnnotation {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ClassAnnotation {

    Class<?> classValue();

    Class<?>[] classArrayValue();

  }

}
