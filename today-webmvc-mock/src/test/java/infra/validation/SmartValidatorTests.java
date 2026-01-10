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

package infra.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/28 22:15
 */
class SmartValidatorTests {

  @Test
  void validateValueThrowsExceptionByDefault() {
    SmartValidator validator = new TestSmartValidator();
    SimpleErrors errors = new SimpleErrors("test");

    assertThatThrownBy(() -> validator.validateValue(String.class, "field", "value", errors))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot validate individual value for class java.lang.String");
  }

  @Test
  void unwrapReturnsNullByDefault() {
    SmartValidator validator = new TestSmartValidator();

    assertThat(validator.unwrap(Validator.class)).isNull();
  }

  @Test
  void validateWithHintsCallsValidate() {
    TestSmartValidator validator = new TestSmartValidator();
    Object target = new Object();
    SimpleErrors errors = new SimpleErrors("test");
    Object hint = new Object();

    validator.validate(target, errors, hint);

    assertThat(validator.validatedTarget).isSameAs(target);
    assertThat(validator.validatedErrors).isSameAs(errors);
    assertThat(validator.validationHints).containsExactly(hint);
  }

  @Test
  void validateWithMultipleHintsPassesAllHints() {
    TestSmartValidator validator = new TestSmartValidator();
    Object target = new Object();
    SimpleErrors errors = new SimpleErrors("test");
    Object hint1 = new Object();
    Object hint2 = new Object();

    validator.validate(target, errors, hint1, hint2);

    assertThat(validator.validationHints).containsExactly(hint1, hint2);
  }

  private static class TestSmartValidator implements SmartValidator {
    Object validatedTarget;
    Errors validatedErrors;
    Object[] validationHints;

    @Override
    public boolean supports(Class<?> clazz) {
      return true;
    }

    @Override
    public void validate(Object target, Errors errors) {
      validatedTarget = target;
      validatedErrors = errors;
    }

    @Override
    public void validate(Object target, Errors errors, Object... validationHints) {
      validate(target, errors);
      this.validationHints = validationHints;
    }
  }

}