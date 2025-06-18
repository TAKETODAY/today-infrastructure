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