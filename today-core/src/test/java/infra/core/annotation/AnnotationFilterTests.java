/*
 * Copyright 2017 - 2026 the original author or authors.
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
