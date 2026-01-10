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

package infra.core.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.concurrent.ThreadSafe;

import infra.lang.Contract;
import infra.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AnnotationFilter}.
 *
 * @author Phillip Webb
 */
class AnnotationFilterTests {

  private static final AnnotationFilter FILTER = annotationType ->
          ObjectUtils.nullSafeEquals(annotationType, TestAnnotation.class.getName());

  @Test
  void matchesAnnotationWhenMatchReturnsTrue() {
    TestAnnotation annotation = WithTestAnnotation.class.getDeclaredAnnotation(TestAnnotation.class);
    assertThat(FILTER.matches(annotation)).isTrue();
  }

  @Test
  void matchesAnnotationWhenNoMatchReturnsFalse() {
    OtherAnnotation annotation = WithOtherAnnotation.class.getDeclaredAnnotation(OtherAnnotation.class);
    assertThat(FILTER.matches(annotation)).isFalse();
  }

  @Test
  void matchesAnnotationClassWhenMatchReturnsTrue() {
    Class<TestAnnotation> annotationType = TestAnnotation.class;
    assertThat(FILTER.matches(annotationType)).isTrue();
  }

  @Test
  void matchesAnnotationClassWhenNoMatchReturnsFalse() {
    Class<OtherAnnotation> annotationType = OtherAnnotation.class;
    assertThat(FILTER.matches(annotationType)).isFalse();
  }

  @Test
  void plainWhenJavaLangAnnotationReturnsTrue() {
    assertThat(AnnotationFilter.PLAIN.matches(Retention.class)).isTrue();
  }

  @Test
  void plainWhenInfraLangAnnotationReturnsTrue() {
    assertThat(AnnotationFilter.PLAIN.matches(Contract.class)).isTrue();
  }

  @Test
  void plainWhenOtherAnnotationReturnsFalse() {
    assertThat(AnnotationFilter.PLAIN.matches(TestAnnotation.class)).isFalse();
  }

  @Test
  void javaWhenJavaLangAnnotationReturnsTrue() {
    assertThat(AnnotationFilter.JAVA.matches(Retention.class)).isTrue();
  }

  @Test
  void javaWhenJavaxAnnotationReturnsTrue() {
    assertThat(AnnotationFilter.JAVA.matches(ThreadSafe.class)).isTrue();
  }

  @Test
  void javaWhenOtherAnnotationReturnsFalse() {
    assertThat(AnnotationFilter.JAVA.matches(TestAnnotation.class)).isFalse();
  }

  @Test
  void noneReturnsFalse() {
    assertThat(AnnotationFilter.NONE.matches(Retention.class)).isFalse();
    assertThat(AnnotationFilter.NONE.matches(TestAnnotation.class)).isFalse();
  }

  @Test
  void packagesFilterMatchesAnnotationInSpecifiedPackage() {
    AnnotationFilter filter = AnnotationFilter.packages("java.lang");
    assertThat(filter.matches(Retention.class)).isTrue();
  }

  @Test
  void packagesFilterDoesNotMatchAnnotationOutsideSpecifiedPackage() {
    AnnotationFilter filter = AnnotationFilter.packages("java.lang");
    assertThat(filter.matches(TestAnnotation.class)).isFalse();
  }

  @Test
  void packagesFilterMatchesAnnotationByTypeName() {
    AnnotationFilter filter = AnnotationFilter.packages("java.lang");
    assertThat(filter.matches("java.lang.Retention")).isTrue();
    assertThat(filter.matches("infra.core.annotation.AnnotationFilterTests$TestAnnotation")).isFalse();
  }

  @Test
  void packagesFilterWorksWithMultiplePackages() {
    AnnotationFilter filter = AnnotationFilter.packages("java.lang", "infra.lang");
    assertThat(filter.matches(Retention.class)).isTrue();
    assertThat(filter.matches(Contract.class)).isTrue();
    assertThat(filter.matches(TestAnnotation.class)).isFalse();
  }

  @Test
  void allFilterMatchesEverything() {
    assertThat(AnnotationFilter.ALL.matches(Retention.class)).isTrue();
    assertThat(AnnotationFilter.ALL.matches(mock(Retention.class))).isTrue();
    assertThat(AnnotationFilter.ALL.matches(TestAnnotation.class)).isTrue();
    assertThat(AnnotationFilter.ALL.matches("any.package.AnyAnnotation")).isTrue();
  }

  @Test
  void noneFilterMatchesNothing() {
    assertThat(AnnotationFilter.NONE.matches(Retention.class)).isFalse();
    assertThat(AnnotationFilter.NONE.matches(TestAnnotation.class)).isFalse();
    assertThat(AnnotationFilter.NONE.matches(mock(TestAnnotation.class))).isFalse();
    assertThat(AnnotationFilter.NONE.matches("any.package.AnyAnnotation")).isFalse();
  }

  @Test
  void plainFilterExcludesInfraLang() {
    assertThat(AnnotationFilter.PLAIN.matches(Contract.class)).isTrue();
  }

  @Test
  void plainFilterExcludesJavaLang() {
    assertThat(AnnotationFilter.PLAIN.matches(Retention.class)).isTrue();
  }

  @Test
  void plainFilterDoesNotMatchCustomAnnotations() {
    assertThat(AnnotationFilter.PLAIN.matches(TestAnnotation.class)).isFalse();
  }

  @Test
  void javaFilterIncludesJavaPackages() {
    assertThat(AnnotationFilter.JAVA.matches(Retention.class)).isTrue();
    assertThat(AnnotationFilter.JAVA.matches(ThreadSafe.class)).isTrue();
  }

  @Test
  void javaFilterDoesNotMatchCustomAnnotations() {
    assertThat(AnnotationFilter.JAVA.matches(TestAnnotation.class)).isFalse();
  }

  @Test
  void matchesAnnotationByClass() {
    assertThat(AnnotationFilter.JAVA.matches(Retention.class)).isTrue();
  }

  @Test
  void matchesAnnotationByTypeNameString() {
    assertThat(AnnotationFilter.JAVA.matches("java.lang.annotation.Retention")).isTrue();
  }

  @Test
  void customFilterMatchesByTypeName() {
    AnnotationFilter customFilter = "infra.core.annotation.AnnotationFilterTests$TestAnnotation"::equals;

    assertThat(customFilter.matches(TestAnnotation.class)).isTrue();
    assertThat(customFilter.matches(OtherAnnotation.class)).isFalse();
  }

  @Test
  void customFilterMatchesByClass() {
    AnnotationFilter customFilter = annotationType ->
            TestAnnotation.class.getName().equals(annotationType);

    TestAnnotation annotation = WithTestAnnotation.class.getAnnotation(TestAnnotation.class);
    assertThat(customFilter.matches(annotation)).isTrue();
  }

  @Test
  void plainToStringReturnsExpectedValue() {
    assertThat(AnnotationFilter.PLAIN.toString()).contains("java.lang");
  }

  @Test
  void javaToStringReturnsExpectedValue() {
    assertThat(AnnotationFilter.JAVA.toString()).contains("java");
  }

  @Test
  void allToStringReturnsExpectedValue() {
    assertThat(AnnotationFilter.ALL.toString()).isEqualTo("All annotations filtered");
  }

  @Test
  void noneToStringReturnsExpectedValue() {
    assertThat(AnnotationFilter.NONE.toString()).isEqualTo("No annotation filtering");
  }

  @Test
  void packagesFilterToStringReturnsExpectedValue() {
    AnnotationFilter filter = AnnotationFilter.packages("java.lang", "infra.lang");
    assertThat(filter.toString()).contains("java.lang", "infra.lang");
  }

  @Test
  void packagesFilterWithSinglePackage() {
    AnnotationFilter filter = AnnotationFilter.packages("java.lang.annotation");
    assertThat(filter.matches(Retention.class)).isTrue();
    assertThat(filter.matches(Contract.class)).isFalse();
  }

  @Test
  void packagesFilterWithEmptyPackagesArray() {
    AnnotationFilter filter = AnnotationFilter.packages();
    assertThat(filter.matches(Retention.class)).isFalse();
    assertThat(filter.matches(TestAnnotation.class)).isFalse();
  }

  @Test
  void packagesFilterWorksWithJakartaAnnotations() {
    AnnotationFilter filter = AnnotationFilter.packages("jakarta");
    // This would match jakarta annotations if they were in classpath
    assertThat(filter.matches("jakarta.annotation.Resource")).isTrue();
    assertThat(filter.matches("java.lang.Override")).isFalse();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface TestAnnotation {
  }

  @TestAnnotation
  static class WithTestAnnotation {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface OtherAnnotation {
  }

  @OtherAnnotation
  static class WithOtherAnnotation {
  }

}
