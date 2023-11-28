/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.validation;

import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.framework.test.context.FilteredClassLoader;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.validation.Errors;
import cn.taketoday.validation.MapBindingResult;
import cn.taketoday.validation.SmartValidator;
import cn.taketoday.validation.beanvalidation.LocalValidatorFactoryBean;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/23 21:25
 */
class ValidatorAdapterTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void wrapLocalValidatorFactoryBean() {
    this.contextRunner.withUserConfiguration(LocalValidatorFactoryBeanConfig.class).run((context) -> {
      ValidatorAdapter wrapper = context.getBean(ValidatorAdapter.class);
      assertThat(wrapper.supports(SampleData.class)).isTrue();
      MapBindingResult errors = new MapBindingResult(new HashMap<String, Object>(), "test");
      wrapper.validate(new SampleData(40), errors);
      assertThat(errors.getErrorCount()).isOne();
    });
  }

  @Test
  void wrapperInvokesCallbackOnNonManagedBean() {
    this.contextRunner.withUserConfiguration(NonManagedBeanConfig.class).run((context) -> {
      LocalValidatorFactoryBean validator = context.getBean(NonManagedBeanConfig.class).validator;
      then(validator).should().setApplicationContext(any(ApplicationContext.class));
      then(validator).should().afterPropertiesSet();
      then(validator).should(never()).destroy();
      context.close();
      then(validator).should().destroy();
    });
  }

  @Test
  void wrapperDoesNotInvokeCallbackOnManagedBean() {
    this.contextRunner.withUserConfiguration(ManagedBeanConfig.class).run((context) -> {
      LocalValidatorFactoryBean validator = context.getBean(ManagedBeanConfig.class).validator;
      then(validator).should(never()).setApplicationContext(any(ApplicationContext.class));
      then(validator).should(never()).afterPropertiesSet();
      then(validator).should(never()).destroy();
      context.close();
      then(validator).should(never()).destroy();
    });
  }

  @Test
  void wrapperWhenValidationProviderNotPresentShouldNotThrowException() {
    ClassPathResource hibernateValidator = new ClassPathResource(
            "META-INF/services/jakarta.validation.spi.ValidationProvider");
    this.contextRunner
            .withClassLoader(new FilteredClassLoader(FilteredClassLoader.ClassPathResourceFilter.of(hibernateValidator),
                    FilteredClassLoader.PackageFilter.of("org.hibernate.validator")))
            .run((context) -> ValidatorAdapter.get(context, null));
  }

  @Test
  void unwrapToJakartaValidatorShouldReturnJakartaValidator() {
    this.contextRunner.withUserConfiguration(LocalValidatorFactoryBeanConfig.class).run((context) -> {
      ValidatorAdapter wrapper = context.getBean(ValidatorAdapter.class);
      assertThat(wrapper.unwrap(Validator.class)).isInstanceOf(Validator.class);
    });
  }

  @Test
  void whenJakartaValidatorIsWrappedMultipleTimesUnwrapToJakartaValidatorShouldReturnJakartaValidator() {
    this.contextRunner.withUserConfiguration(DoubleWrappedConfig.class).run((context) -> {
      ValidatorAdapter wrapper = context.getBean(ValidatorAdapter.class);
      assertThat(wrapper.unwrap(Validator.class)).isInstanceOf(Validator.class);
    });
  }

  @Test
  void unwrapToUnsupportedTypeShouldThrow() {
    this.contextRunner.withUserConfiguration(LocalValidatorFactoryBeanConfig.class).run((context) -> {
      ValidatorAdapter wrapper = context.getBean(ValidatorAdapter.class);
      assertThatRuntimeException().isThrownBy(() -> wrapper.unwrap(HibernateValidator.class));
    });
  }

  @Configuration(proxyBeanMethods = false)
  static class LocalValidatorFactoryBeanConfig {

    @Bean
    LocalValidatorFactoryBean validator() {
      return new LocalValidatorFactoryBean();
    }

    @Bean
    ValidatorAdapter wrapper(LocalValidatorFactoryBean validator) {
      return new ValidatorAdapter(validator, true);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class DoubleWrappedConfig {

    @Bean
    LocalValidatorFactoryBean validator() {
      return new LocalValidatorFactoryBean();
    }

    @Bean
    ValidatorAdapter wrapper(LocalValidatorFactoryBean validator) {
      return new ValidatorAdapter(new Wrapper(validator), true);
    }

    static class Wrapper implements SmartValidator {

      private final SmartValidator delegate;

      Wrapper(SmartValidator delegate) {
        this.delegate = delegate;
      }

      @Override
      public boolean supports(Class<?> clazz) {
        return this.delegate.supports(clazz);
      }

      @Override
      public void validate(Object target, Errors errors) {
        this.delegate.validate(target, errors);
      }

      @Override
      public void validate(Object target, Errors errors, Object... validationHints) {
        this.delegate.validate(target, errors, validationHints);
      }

      @Override
      @SuppressWarnings("unchecked")
      public <T> T unwrap(Class<T> type) {
        if (type.isInstance(this.delegate)) {
          return (T) this.delegate;
        }
        return this.delegate.unwrap(type);
      }

    }

  }

  @Configuration(proxyBeanMethods = false)
  static class NonManagedBeanConfig {

    private final LocalValidatorFactoryBean validator = mock(LocalValidatorFactoryBean.class);

    @Bean
    ValidatorAdapter wrapper() {
      return new ValidatorAdapter(this.validator, false);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ManagedBeanConfig {

    private final LocalValidatorFactoryBean validator = mock(LocalValidatorFactoryBean.class);

    @Bean
    ValidatorAdapter wrapper() {
      return new ValidatorAdapter(this.validator, true);
    }

  }

  static class SampleData {

    @Min(42)
    private final int counter;

    SampleData(int counter) {
      this.counter = counter;
    }

  }

}