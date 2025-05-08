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

import org.aopalliance.aop.Advice;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import infra.aop.Advisor;
import infra.lang.Nullable;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/5/8 11:13
 */
class MethodValidationPostProcessorTests {

  @Test
  void validatedAnnotationTypeCannotBeNull() {
    MethodValidationPostProcessor processor = new MethodValidationPostProcessor();

    assertThatIllegalArgumentException()
            .isThrownBy(() -> processor.setValidatedAnnotationType(null))
            .withMessage("'validatedAnnotationType' is required");
  }

  @Test
  void canSetCustomValidatedAnnotationType() {
    MethodValidationPostProcessor0 processor = new MethodValidationPostProcessor0();
    processor.setValidatedAnnotationType(MyValidated.class);
    processor.afterPropertiesSet();

    // Advisor is created with custom annotation type
    assertNotNull(processor.getAdvisor());
  }

  @Test
  void canSetValidator() {
    MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    processor.setValidator(validator);
  }

  @Test
  void canSetValidatorFactory() {
    MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    processor.setValidatorFactory(factory);
  }

  @Test
  void canSetValidatorProvider() {
    MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
    Supplier<Validator> provider = () -> Validation.buildDefaultValidatorFactory().getValidator();
    processor.setValidatorProvider(provider);
  }

  @Test
  void defaultsToValidatedAnnotation() {
    MethodValidationPostProcessor0 processor = new MethodValidationPostProcessor0();
    processor.afterPropertiesSet();

    assertInstanceOf(MethodValidationInterceptor.class, processor.getAdvisor().getAdvice());
  }

  @Test
  void adaptConstraintViolationsDefaultsFalse() {
    MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
    MethodValidationInterceptor interceptor =
            (MethodValidationInterceptor) processor.createMethodValidationAdvice(null);

    assertFalse(interceptor.adaptViolations);
  }

  @Test
  void canEnableAdaptingConstraintViolations() {
    MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
    processor.setAdaptConstraintViolations(true);

    MethodValidationInterceptor interceptor =
            (MethodValidationInterceptor) processor.createMethodValidationAdvice(null);

    assertTrue(interceptor.adaptViolations);
  }

  @Test
  void handlesLocalValidatorFactoryBeanUnwrapping() {
    MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
    LocalValidatorFactoryBean validatorBean = new LocalValidatorFactoryBean();
    validatorBean.afterPropertiesSet();
    processor.setValidator(validatorBean);
  }

  @Test
  void handlesInfraValidatorAdapterUnwrapping() {
    MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
    InfraValidatorAdapter validatorAdapter = new InfraValidatorAdapter(
            Validation.buildDefaultValidatorFactory().getValidator());
    processor.setValidator(validatorAdapter);
  }

  @Test
  void handlesNullValidatorInAdviceCreation() {
    MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
    Advice advice = processor.createMethodValidationAdvice(null);
    assertInstanceOf(MethodValidationInterceptor.class, advice);
  }

  @Test
  void beforeExistingAdvisorsDefaultsFalse() {
    MethodValidationPostProcessor0 processor = new MethodValidationPostProcessor0();
    assertFalse(processor.getBeforeExistingAdvisors());
  }

  @Test
  void canEnableBeforeExistingAdvisors() {
    MethodValidationPostProcessor0 processor = new MethodValidationPostProcessor0();
    processor.setBeforeExistingAdvisors(true);
    assertTrue(processor.getBeforeExistingAdvisors());
  }

  @Test
  void validatorInitializedAfterPropertiesSet() {
    MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
    processor.afterPropertiesSet();

    MethodValidationInterceptor interceptor =
            (MethodValidationInterceptor) processor.createMethodValidationAdvice(null);
    assertNotNull(interceptor);
  }

  @Target({ ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @interface MyValidated {
  }

  static class MethodValidationPostProcessor0 extends MethodValidationPostProcessor {

    @Nullable
    public Advisor getAdvisor() {
      return advisor;
    }

    boolean getBeforeExistingAdvisors() {
      return beforeExistingAdvisors;
    }

  }

}