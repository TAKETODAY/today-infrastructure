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

import infra.beans.testfixture.beans.TestBean;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorContext;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotEmpty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/5/8 10:37
 */
class CustomValidatorBeanTests {

  @Test
  void defaultValidatorCreatedWhenNoFactoryProvided() {
    CustomValidatorBean validatorBean = new CustomValidatorBean();
    validatorBean.afterPropertiesSet();

    Validator validator = validatorBean.targetValidator;
    assertThat(validator).isNotNull();
  }

  @Test
  void customValidatorFactoryUsedWhenProvided() {
    ValidatorFactory mockFactory = mock(ValidatorFactory.class);
    ValidatorContext mockContext = mock(ValidatorContext.class);
    Validator mockValidator = mock(Validator.class);

    when(mockFactory.usingContext()).thenReturn(mockContext);
    when(mockFactory.getMessageInterpolator()).thenReturn(mock(MessageInterpolator.class));
    when(mockContext.messageInterpolator(any())).thenReturn(mockContext);
    when(mockContext.getValidator()).thenReturn(mockValidator);

    CustomValidatorBean validatorBean = new CustomValidatorBean();
    validatorBean.setValidatorFactory(mockFactory);
    validatorBean.afterPropertiesSet();

    assertThat(validatorBean.targetValidator).isSameAs(mockValidator);
  }

  @Test
  void customMessageInterpolatorUsedWhenProvided() {
    MessageInterpolator customInterpolator = mock(MessageInterpolator.class);
    ValidatorFactory mockFactory = mock(ValidatorFactory.class);
    ValidatorContext mockContext = mock(ValidatorContext.class);

    when(mockFactory.usingContext()).thenReturn(mockContext);
    when(mockContext.messageInterpolator(any())).thenReturn(mockContext);
    when(mockContext.getValidator()).thenReturn(mock(Validator.class));

    CustomValidatorBean validatorBean = new CustomValidatorBean();
    validatorBean.setValidatorFactory(mockFactory);
    validatorBean.setMessageInterpolator(customInterpolator);
    validatorBean.afterPropertiesSet();

    verify(mockContext).messageInterpolator(any(LocaleContextMessageInterpolator.class));
  }

  @Test
  void customTraversableResolverUsedWhenProvided() {
    TraversableResolver customResolver = mock(TraversableResolver.class);
    ValidatorFactory mockFactory = mock(ValidatorFactory.class);
    ValidatorContext mockContext = mock(ValidatorContext.class);

    when(mockFactory.usingContext()).thenReturn(mockContext);
    when(mockFactory.getMessageInterpolator()).thenReturn(mock(MessageInterpolator.class));
    when(mockContext.messageInterpolator(any())).thenReturn(mockContext);
    when(mockContext.traversableResolver(any())).thenReturn(mockContext);
    when(mockContext.getValidator()).thenReturn(mock(Validator.class));

    CustomValidatorBean validatorBean = new CustomValidatorBean();
    validatorBean.setValidatorFactory(mockFactory);
    validatorBean.setTraversableResolver(customResolver);
    validatorBean.afterPropertiesSet();

    verify(mockContext).traversableResolver(customResolver);
  }

  @Test
  void validationOfSimpleBean() {
    CustomValidatorBean validatorBean = new CustomValidatorBean();
    validatorBean.afterPropertiesSet();

    TestBean testBean = new TestBean();
    Set<ConstraintViolation<TestBean>> violations = validatorBean.validate(testBean);

    assertThat(violations).isEmpty();
  }

  @Test
  void validationWithConstraintViolations() {
    CustomValidatorBean validatorBean = new CustomValidatorBean();
    validatorBean.afterPropertiesSet();

    TestBeanName testBean = new TestBeanName();
    testBean.setName("");

    Set<ConstraintViolation<TestBeanName>> violations = validatorBean.validate(testBean);

    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Name cannot be empty");
  }

  static class TestBeanName {

    @NotEmpty(message = "Name cannot be empty")
    private String name;

    public void setName(String s) {
      this.name = s;
    }

    public String getName() {
      return name;
    }
  }

}