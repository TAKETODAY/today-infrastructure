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

package infra.validation.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import infra.core.annotation.AnnotationUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/27 22:39
 */
class ValidationAnnotationUtilsTests {

  @Test
  void shouldReturnNullForNonValidationAnnotation() {
    NonValidation annotation = AnnotationUtils.synthesizeAnnotation(NonValidation.class);
    assertThat(ValidationAnnotationUtils.determineValidationHints(annotation)).isNull();
  }

  @Test
  void shouldReturnEmptyArrayForAtValidAnnotation() {
    Valid annotation = AnnotationUtils.synthesizeAnnotation(Valid.class);
    assertThat(ValidationAnnotationUtils.determineValidationHints(annotation)).isEmpty();
  }

  @Test
  void shouldReturnValueFromValidatedAnnotation() {
    @Validated(String.class)
    class Sample { }
    Validated annotation = Sample.class.getAnnotation(Validated.class);
    assertThat(ValidationAnnotationUtils.determineValidationHints(annotation))
            .containsExactly(String.class);
  }

  @Test
  void shouldReturnEmptyArrayForValidatedAnnotationWithoutValue() {
    @Validated
    class Sample { }
    Validated annotation = Sample.class.getAnnotation(Validated.class);
    assertThat(ValidationAnnotationUtils.determineValidationHints(annotation)).isEmpty();
  }

  @Test
  void shouldReturnEmptyArrayFromCustomValidAnnotationWithoutValue() {
    ValidCustom annotation = AnnotationUtils.synthesizeAnnotation(ValidCustom.class);
    assertThat(ValidationAnnotationUtils.determineValidationHints(annotation)).isEmpty();
  }

  @Test
  void shouldReturnMetaValidatedValue() {
    CustomValidated annotation = AnnotationUtils.synthesizeAnnotation(CustomValidated.class);
    assertThat(ValidationAnnotationUtils.determineValidationHints(annotation))
            .containsExactly(Integer.class);
  }

  @Test
  void shouldHandleComposedValidationAnnotations() {

    @CustomValidation
    class Sample {
    }

    CustomValidation annotation = Sample.class.getAnnotation(CustomValidation.class);
    assertThat(ValidationAnnotationUtils.determineValidationHints(annotation))
            .containsExactly(String.class);
  }

  @Test
  void shouldHandleMultipleValidationHints() {
    @Validated({ String.class, Integer.class })
    class Sample { }
    Validated annotation = Sample.class.getAnnotation(Validated.class);
    assertThat(ValidationAnnotationUtils.determineValidationHints(annotation))
            .containsExactly(String.class, Integer.class);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Validated(String.class)
  @interface CustomValidation {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Validated(Long.class)
  @interface OuterValidated {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @OuterValidated
  @interface InnerValidated {
  }

  @Retention(RetentionPolicy.RUNTIME)
  private @interface NonValidation { }

  @Retention(RetentionPolicy.RUNTIME)
  private @interface Valid { }

  @Retention(RetentionPolicy.RUNTIME)
  private @interface ValidCustom {
    String[] value() default {};
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Validated(Integer.class)
  private @interface CustomValidated { }
}
