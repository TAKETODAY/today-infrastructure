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