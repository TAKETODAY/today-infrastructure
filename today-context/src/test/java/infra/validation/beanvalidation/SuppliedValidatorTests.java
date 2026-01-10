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

package infra.validation.beanvalidation;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.BeanDescriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/5/8 11:10
 */
class SuppliedValidatorTests {

  @Test
  void missingSupplierThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new SuppliedValidator(null))
            .withMessage("validatorSupplier is required");
  }

  @Test
  void validatorIsCachedAfterFirstAccess() {
    AtomicInteger supplierCallCount = new AtomicInteger(0);
    Validator mockValidator = mock(Validator.class);
    Supplier<Validator> supplier = () -> {
      supplierCallCount.incrementAndGet();
      return mockValidator;
    };

    SuppliedValidator validator = new SuppliedValidator(supplier);

    validator.getValidator();
    validator.getValidator();
    validator.getValidator();

    assertThat(supplierCallCount.get()).isEqualTo(1);
  }

  @Test
  void validateDelegatesToSuppliedValidator() {
    Object testObject = new Object();
    Set<ConstraintViolation<Object>> expectedViolations = Set.of();
    Validator mockValidator = mock(Validator.class);
    when(mockValidator.validate(testObject)).thenReturn(expectedViolations);

    SuppliedValidator validator = new SuppliedValidator(() -> mockValidator);
    Set<ConstraintViolation<Object>> violations = validator.validate(testObject);

    assertThat(violations).isSameAs(expectedViolations);
    verify(mockValidator).validate(testObject);
  }

  @Test
  void validatePropertyDelegatesToSuppliedValidator() {
    Object testObject = new Object();
    String propertyName = "test";
    Set<ConstraintViolation<Object>> expectedViolations = Set.of();
    Validator mockValidator = mock(Validator.class);
    when(mockValidator.validateProperty(testObject, propertyName)).thenReturn(expectedViolations);

    SuppliedValidator validator = new SuppliedValidator(() -> mockValidator);
    Set<ConstraintViolation<Object>> violations = validator.validateProperty(testObject, propertyName);

    assertThat(violations).isSameAs(expectedViolations);
    verify(mockValidator).validateProperty(testObject, propertyName);
  }

  @Test
  void validateValueDelegatesToSuppliedValidator() {
    Class<String> beanType = String.class;
    String propertyName = "test";
    Object value = "value";
    Set<ConstraintViolation<String>> expectedViolations = Set.of();
    Validator mockValidator = mock(Validator.class);
    when(mockValidator.validateValue(beanType, propertyName, value)).thenReturn(expectedViolations);

    SuppliedValidator validator = new SuppliedValidator(() -> mockValidator);
    Set<ConstraintViolation<String>> violations = validator.validateValue(beanType, propertyName, value);

    assertThat(violations).isSameAs(expectedViolations);
    verify(mockValidator).validateValue(beanType, propertyName, value);
  }

  @Test
  void getConstraintsForClassDelegatesToSuppliedValidator() {
    Class<?> beanClass = String.class;
    BeanDescriptor expectedDescriptor = mock(BeanDescriptor.class);
    Validator mockValidator = mock(Validator.class);
    when(mockValidator.getConstraintsForClass(beanClass)).thenReturn(expectedDescriptor);

    SuppliedValidator validator = new SuppliedValidator(() -> mockValidator);
    BeanDescriptor descriptor = validator.getConstraintsForClass(beanClass);

    assertThat(descriptor).isSameAs(expectedDescriptor);
    verify(mockValidator).getConstraintsForClass(beanClass);
  }

  @Test
  void forExecutablesDelegatesToSuppliedValidator() {
    ExecutableValidator expectedExecutableValidator = mock(ExecutableValidator.class);
    Validator mockValidator = mock(Validator.class);
    when(mockValidator.forExecutables()).thenReturn(expectedExecutableValidator);

    SuppliedValidator validator = new SuppliedValidator(() -> mockValidator);
    ExecutableValidator executableValidator = validator.forExecutables();

    assertThat(executableValidator).isSameAs(expectedExecutableValidator);
    verify(mockValidator).forExecutables();
  }

  @Test
  void unwrapToValidatorTypeReturnsSelf() {
    SuppliedValidator validator = new SuppliedValidator(() -> mock(Validator.class));

    Validator unwrapped = validator.unwrap(Validator.class);

    assertThat(unwrapped).isSameAs(validator);
  }

  @Test
  void unwrapToOtherTypeDelegatesToSuppliedValidator() {
    String unwrappedValue = "test";
    Validator mockValidator = mock(Validator.class);
    when(mockValidator.unwrap(String.class)).thenReturn(unwrappedValue);

    SuppliedValidator validator = new SuppliedValidator(() -> mockValidator);
    String unwrapped = validator.unwrap(String.class);

    assertThat(unwrapped).isSameAs(unwrappedValue);
    verify(mockValidator).unwrap(String.class);
  }

}