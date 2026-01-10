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

import org.aopalliance.aop.Advice;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import infra.aop.Advisor;
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