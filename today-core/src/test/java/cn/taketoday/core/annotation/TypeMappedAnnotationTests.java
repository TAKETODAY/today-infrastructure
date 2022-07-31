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

package cn.taketoday.core.annotation;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TypeMappedAnnotation}. See also
 * {@link cn.taketoday.core.annotation.MergedAnnotationsTests} for a much more extensive collection of tests.
 *
 * @author Phillip Webb
 */
class TypeMappedAnnotationTests {

  @Test
  void mappingWhenMirroredReturnsMirroredValues() {
    testExplicitMirror(WithExplicitMirrorA.class);
    testExplicitMirror(WithExplicitMirrorB.class);
  }

  private void testExplicitMirror(Class<?> annotatedClass) {
    TypeMappedAnnotation<ExplicitMirror> annotation = getTypeMappedAnnotation(
            annotatedClass, ExplicitMirror.class);
    assertThat(annotation.getString("a")).isEqualTo("test");
    assertThat(annotation.getString("b")).isEqualTo("test");
  }

  @Test
  void mappingExplicitAliasToMetaAnnotationReturnsMappedValues() {
    TypeMappedAnnotation<?> annotation = getTypeMappedAnnotation(
            WithExplicitAliasToMetaAnnotation.class,
            ExplicitAliasToMetaAnnotation.class,
            ExplicitAliasMetaAnnotationTarget.class);
    assertThat(annotation.getString("aliased")).isEqualTo("aliased");
    assertThat(annotation.getString("nonAliased")).isEqualTo("nonAliased");
  }

  @Test
  void mappingConventionAliasToMetaAnnotationReturnsMappedValues() {
    TypeMappedAnnotation<?> annotation = getTypeMappedAnnotation(
            WithConventionAliasToMetaAnnotation.class,
            ConventionAliasToMetaAnnotation.class,
            ConventionAliasMetaAnnotationTarget.class);
    assertThat(annotation.getString("value")).isEqualTo("");
    assertThat(annotation.getString("convention")).isEqualTo("convention");
  }

  @Test
  void adaptFromEmptyArrayToAnyComponentType() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(ArrayTypes.class);
    Map<String, Object> attributes = new HashMap<>();
    for (int i = 0; i < methods.size(); i++) {
      attributes.put(methods.get(i).getName(), new Object[] {});
    }
    MergedAnnotation<ArrayTypes> annotation = TypeMappedAnnotation.of(null, null,
            ArrayTypes.class, attributes);
    assertThat(annotation.getValue("stringValue")).contains(new String[] {});
    assertThat(annotation.getValue("byteValue")).contains(new byte[] {});
    assertThat(annotation.getValue("shortValue")).contains(new short[] {});
    assertThat(annotation.getValue("intValue")).contains(new int[] {});
    assertThat(annotation.getValue("longValue")).contains(new long[] {});
    assertThat(annotation.getValue("booleanValue")).contains(new boolean[] {});
    assertThat(annotation.getValue("charValue")).contains(new char[] {});
    assertThat(annotation.getValue("doubleValue")).contains(new double[] {});
    assertThat(annotation.getValue("floatValue")).contains(new float[] {});
    assertThat(annotation.getValue("classValue")).contains(new Class<?>[] {});
    assertThat(annotation.getValue("annotationValue")).contains(new MergedAnnotation<?>[] {});
    assertThat(annotation.getValue("enumValue")).contains(new ExampleEnum[] {});
  }

  @Test
  void adaptFromNestedMergedAnnotation() {
    MergedAnnotation<Nested> nested = MergedAnnotation.valueOf(Nested.class);
    MergedAnnotation<?> annotation = TypeMappedAnnotation.of(null, null,
            NestedContainer.class, Collections.singletonMap("value", nested));
    assertThat(annotation.getAnnotation("value", Nested.class)).isSameAs(nested);
  }

  @Test
  void adaptFromStringToClass() {
    MergedAnnotation<?> annotation = TypeMappedAnnotation.of(null, null,
            ClassAttributes.class,
            Collections.singletonMap("classValue", InputStream.class.getName()));
    assertThat(annotation.getString("classValue")).isEqualTo(InputStream.class.getName());
    assertThat(annotation.getClass("classValue")).isEqualTo(InputStream.class);
  }

  @Test
  void adaptFromStringArrayToClassArray() {
    MergedAnnotation<?> annotation = TypeMappedAnnotation.of(null, null, ClassAttributes.class,
            Collections.singletonMap("classArrayValue", new String[] { InputStream.class.getName() }));
    assertThat(annotation.getStringArray("classArrayValue")).containsExactly(InputStream.class.getName());
    Class<?>[] classArrayValues = annotation.getClassArray("classArrayValue");
    assertThat(classArrayValues).containsExactly(InputStream.class);
  }

  private <A extends Annotation> TypeMappedAnnotation<A> getTypeMappedAnnotation(
          Class<?> source, Class<A> annotationType) {
    return getTypeMappedAnnotation(source, annotationType, annotationType);
  }

  private <A extends Annotation> TypeMappedAnnotation<A> getTypeMappedAnnotation(
          Class<?> source, Class<? extends Annotation> rootAnnotationType,
          Class<A> annotationType) {
    Annotation rootAnnotation = source.getAnnotation(rootAnnotationType);
    AnnotationTypeMapping mapping = getMapping(rootAnnotation, annotationType);
    return TypeMappedAnnotation.createIfPossible(mapping, source, rootAnnotation, 0, IntrospectionFailureLogger.INFO);
  }

  private AnnotationTypeMapping getMapping(Annotation annotation,
          Class<? extends Annotation> mappedAnnotationType) {
    AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
            annotation.annotationType());
    for (int i = 0; i < mappings.size(); i++) {
      AnnotationTypeMapping candidate = mappings.get(i);
      if (candidate.annotationType.equals(mappedAnnotationType)) {
        return candidate;
      }
    }
    throw new IllegalStateException(
            "No mapping from " + annotation + " to " + mappedAnnotationType);
  }

  @Retention(RetentionPolicy.RUNTIME)
  static @interface ExplicitMirror {

    @AliasFor("b")
    String a() default "";

    @AliasFor("a")
    String b() default "";

  }

  @ExplicitMirror(a = "test")
  static class WithExplicitMirrorA {

  }

  @ExplicitMirror(b = "test")
  static class WithExplicitMirrorB {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @ExplicitAliasMetaAnnotationTarget(nonAliased = "nonAliased")
  static @interface ExplicitAliasToMetaAnnotation {

    @AliasFor(annotation = ExplicitAliasMetaAnnotationTarget.class)
    String aliased() default "";

  }

  @Retention(RetentionPolicy.RUNTIME)
  static @interface ExplicitAliasMetaAnnotationTarget {

    String aliased() default "";

    String nonAliased() default "";

  }

  @ExplicitAliasToMetaAnnotation(aliased = "aliased")
  private static class WithExplicitAliasToMetaAnnotation {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @ConventionAliasMetaAnnotationTarget
  static @interface ConventionAliasToMetaAnnotation {

    String value() default "";

    String convention() default "";

  }

  @Retention(RetentionPolicy.RUNTIME)
  static @interface ConventionAliasMetaAnnotationTarget {

    String value() default "";

    String convention() default "";

  }

  @ConventionAliasToMetaAnnotation(value = "value", convention = "convention")
  private static class WithConventionAliasToMetaAnnotation {

  }

  @Retention(RetentionPolicy.RUNTIME)
  static @interface ArrayTypes {

    String[] stringValue();

    byte[] byteValue();

    short[] shortValue();

    int[] intValue();

    long[] longValue();

    boolean[] booleanValue();

    char[] charValue();

    double[] doubleValue();

    float[] floatValue();

    Class<?>[] classValue();

    ExplicitMirror[] annotationValue();

    ExampleEnum[] enumValue();

  }

  enum ExampleEnum {ONE, TWO, THREE}

  @Retention(RetentionPolicy.RUNTIME)
  static @interface NestedContainer {

    Nested value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  static @interface Nested {

    String value() default "";

  }

  @Retention(RetentionPolicy.RUNTIME)
  static @interface ClassAttributes {

    Class<?> classValue();

    Class<?>[] classArrayValue();

  }

}
