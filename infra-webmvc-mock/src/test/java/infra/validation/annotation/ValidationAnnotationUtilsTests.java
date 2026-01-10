/*
 * Copyright 2017 - 2026 the TODAY authors.
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
