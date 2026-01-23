/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.validation.config;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

import infra.app.test.context.assertj.AssertableApplicationContext;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Primary;
import infra.context.annotation.config.AutoConfigurations;
import infra.core.annotation.Order;
import infra.test.util.ReflectionTestUtils;
import infra.validation.annotation.Validated;
import infra.validation.beanvalidation.CustomValidatorBean;
import infra.validation.beanvalidation.LocalValidatorFactoryBean;
import infra.validation.beanvalidation.MethodValidationExcludeFilter;
import infra.validation.beanvalidation.MethodValidationPostProcessor;
import infra.validation.beanvalidation.OptionalValidatorFactoryBean;
import infra.validation.method.MethodValidationException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/23 21:06
 */
class ValidationAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class));

  @Test
  void validationAutoConfigurationShouldConfigureDefaultValidator() {
    this.contextRunner.run((context) -> {
      assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("defaultValidator");
      assertThat(context.getBeanNamesForType(infra.validation.Validator.class))
              .containsExactly("defaultValidator");
      assertThat(context.getBean(Validator.class)).isInstanceOf(LocalValidatorFactoryBean.class)
              .isEqualTo(context.getBean(infra.validation.Validator.class));
      assertThat(isPrimaryBean(context, "defaultValidator")).isTrue();
    });
  }

  @Test
  void validationAutoConfigurationWhenUserProvidesValidatorShouldBackOff() {
    this.contextRunner.withUserConfiguration(UserDefinedValidatorConfig.class).run((context) -> {
      assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("customValidator");
      assertThat(context.getBeanNamesForType(infra.validation.Validator.class))
              .containsExactly("customValidator");
      assertThat(context.getBean(Validator.class)).isInstanceOf(OptionalValidatorFactoryBean.class)
              .isEqualTo(context.getBean(infra.validation.Validator.class));
      assertThat(isPrimaryBean(context, "customValidator")).isFalse();
    });
  }

  @Test
  void validationAutoConfigurationWhenUserProvidesDefaultValidatorShouldNotEnablePrimary() {
    this.contextRunner.withUserConfiguration(UserDefinedDefaultValidatorConfig.class).run((context) -> {
      assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("defaultValidator");
      assertThat(context.getBeanNamesForType(infra.validation.Validator.class))
              .containsExactly("defaultValidator");
      assertThat(isPrimaryBean(context, "defaultValidator")).isFalse();
    });
  }

  @Test
  void validationAutoConfigurationWhenUserProvidesJsrValidatorShouldBackOff() {
    this.contextRunner.withUserConfiguration(UserDefinedJsrValidatorConfig.class).run((context) -> {
      assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("customValidator");
      assertThat(context.getBeanNamesForType(infra.validation.Validator.class)).isEmpty();
      assertThat(isPrimaryBean(context, "customValidator")).isFalse();
    });
  }

  @Test
  void validationAutoConfigurationWhenUserProvidesInfraValidatorShouldCreateJsrValidator() {
    this.contextRunner.withUserConfiguration(UserDefinedInfraValidatorConfig.class).run((context) -> {
      assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("defaultValidator");
      assertThat(context.getBeanNamesForType(infra.validation.Validator.class))
              .containsExactly("customValidator", "anotherCustomValidator", "defaultValidator");
      assertThat(context.getBean(Validator.class)).isInstanceOf(LocalValidatorFactoryBean.class)
              .isEqualTo(context.getBean(infra.validation.Validator.class));
      assertThat(isPrimaryBean(context, "defaultValidator")).isTrue();
    });
  }

  @Test
  void validationAutoConfigurationWhenUserProvidesPrimaryInfraValidatorShouldRemovePrimaryFlag() {
    this.contextRunner.withUserConfiguration(UserDefinedPrimaryInfraValidatorConfig.class).run((context) -> {
      assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("defaultValidator");
      assertThat(context.getBeanNamesForType(infra.validation.Validator.class))
              .containsExactly("customValidator", "anotherCustomValidator", "defaultValidator");
      assertThat(context.getBean(Validator.class)).isInstanceOf(LocalValidatorFactoryBean.class);
      assertThat(context.getBean(infra.validation.Validator.class))
              .isEqualTo(context.getBean("anotherCustomValidator"));
      assertThat(isPrimaryBean(context, "defaultValidator")).isFalse();
    });
  }

  @Test
  void whenUserProvidesInfraValidatorInParentContextThenAutoConfiguredValidatorIsPrimary() {
    new ApplicationContextRunner().withUserConfiguration(UserDefinedInfraValidatorConfig.class).run((parent) -> {
      this.contextRunner.withParent(parent).run((context) -> {
        assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("defaultValidator");
        assertThat(context.getBeanNamesForType(infra.validation.Validator.class))
                .containsExactly("defaultValidator");
        assertThat(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context.getBeanFactory(),
                infra.validation.Validator.class)).containsExactly("defaultValidator",
                "customValidator", "anotherCustomValidator");
        assertThat(isPrimaryBean(context, "defaultValidator")).isTrue();
      });
    });
  }

  @Test
  void whenUserProvidesPrimaryInfraValidatorInParentContextThenAutoConfiguredValidatorIsPrimary() {
    new ApplicationContextRunner().withUserConfiguration(UserDefinedPrimaryInfraValidatorConfig.class)
            .run((parent) -> {
              this.contextRunner.withParent(parent).run((context) -> {
                assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("defaultValidator");
                assertThat(context.getBeanNamesForType(infra.validation.Validator.class))
                        .containsExactly("defaultValidator");
                assertThat(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context.getBeanFactory(),
                        infra.validation.Validator.class)).containsExactly("defaultValidator",
                        "customValidator", "anotherCustomValidator");
                assertThat(isPrimaryBean(context, "defaultValidator")).isTrue();
              });
            });
  }

  @Test
  void validationIsEnabled() {
    this.contextRunner.withUserConfiguration(SampleService.class).run((context) -> {
      assertThat(context.getBeansOfType(Validator.class)).hasSize(1);
      SampleService service = context.getBean(SampleService.class);
      service.doSomething("Valid");
      assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> service.doSomething("KO"));
    });
  }

  @Test
  void classCanBeExcludedFromValidation() {
    this.contextRunner.withUserConfiguration(ExcludedServiceConfiguration.class).run((context) -> {
      assertThat(context.getBeansOfType(Validator.class)).hasSize(1);
      ExcludedService service = context.getBean(ExcludedService.class);
      service.doSomething("Valid");
      assertThatNoException().isThrownBy(() -> service.doSomething("KO"));
    });
  }

  @Test
  void validationUsesCglibProxy() {
    this.contextRunner.withUserConfiguration(DefaultAnotherSampleService.class).run((context) -> {
      assertThat(context.getBeansOfType(Validator.class)).hasSize(1);
      DefaultAnotherSampleService service = context.getBean(DefaultAnotherSampleService.class);
      service.doSomething(42);
      assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> service.doSomething(2));
    });
  }

  @Test
  void validationCanBeConfiguredToUseJdkProxy() {
    this.contextRunner.withUserConfiguration(AnotherSampleServiceConfiguration.class)
            .withPropertyValues("infra.aop.proxy-target-class=false").run((context) -> {
              assertThat(context.getBeansOfType(Validator.class)).hasSize(1);
              assertThat(context.getBeansOfType(DefaultAnotherSampleService.class)).isEmpty();
              AnotherSampleService service = context.getBean(AnotherSampleService.class);
              service.doSomething(42);
              assertThatExceptionOfType(ConstraintViolationException.class)
                      .isThrownBy(() -> service.doSomething(2));
            });
  }

  @Test
  void validationUseDefaultAdaptToConstraintViolationsValue() {
    this.contextRunner.withUserConfiguration(AnotherSampleServiceConfiguration.class).run((context) -> {
      MethodValidationPostProcessor postProcessor = context.getBean(MethodValidationPostProcessor.class);
      assertThat(postProcessor).hasFieldOrPropertyWithValue("adaptConstraintViolations", false);
    });
  }

  @Test
  void userDefinedMethodValidationPostProcessorTakesPrecedence() {
    this.contextRunner.withUserConfiguration(SampleConfiguration.class).run((context) -> {
      assertThat(context.getBeansOfType(Validator.class)).hasSize(1);
      Object userMethodValidationPostProcessor = context.getBean("testMethodValidationPostProcessor");
      assertThat(context.getBean(MethodValidationPostProcessor.class))
              .isSameAs(userMethodValidationPostProcessor);
      assertThat(context.getBeansOfType(MethodValidationPostProcessor.class)).hasSize(1);
      Object validator = ReflectionTestUtils.getField(userMethodValidationPostProcessor, "validator");
      assertThat(validator).isNull();
    });
  }

  @Test
  void methodValidationPostProcessorValidatorDependencyDoesNotTriggerEarlyInitialization() {
    this.contextRunner.withUserConfiguration(CustomValidatorConfiguration.class)
            .run((context) -> assertThat(context.getBean(CustomValidatorConfiguration.TestBeanPostProcessor.class).postProcessed)
                    .contains("someService"));
  }

  @Test
  void validationIsEnabledInChildContext() {
    this.contextRunner.run((parent) -> new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(SampleService.class).withParent(parent).run((context) -> {
              assertThat(context.getBeansOfType(Validator.class)).hasSize(0);
              assertThat(parent.getBeansOfType(Validator.class)).hasSize(1);
              SampleService service = context.getBean(SampleService.class);
              service.doSomething("Valid");
              assertThatExceptionOfType(ConstraintViolationException.class)
                      .isThrownBy(() -> service.doSomething("KO"));
            }));
  }

  @Test
  void configurationCustomizerBeansAreCalledInOrder() {
    this.contextRunner.withUserConfiguration(ConfigurationCustomizersConfiguration.class).run((context) -> {
      ValidationConfigurationCustomizer customizerOne = context.getBean("customizerOne",
              ValidationConfigurationCustomizer.class);
      ValidationConfigurationCustomizer customizerTwo = context.getBean("customizerTwo",
              ValidationConfigurationCustomizer.class);
      InOrder inOrder = Mockito.inOrder(customizerOne, customizerTwo);
      then(customizerTwo).should(inOrder).customize(any(jakarta.validation.Configuration.class));
      then(customizerOne).should(inOrder).customize(any(jakarta.validation.Configuration.class));
    });
  }

  @Test
  void validationCanBeConfiguredToAdaptConstraintViolations() {
    this.contextRunner.withUserConfiguration(AnotherSampleServiceConfiguration.class)
            .withPropertyValues("infra.validation.method.adapt-constraint-violations=true")
            .run((context) -> {
              assertThat(context.getBeansOfType(Validator.class)).hasSize(1);
              AnotherSampleService service = context.getBean(AnotherSampleService.class);
              service.doSomething(42);
              assertThatExceptionOfType(MethodValidationException.class).isThrownBy(() -> service.doSomething(2));
            });
  }

  private boolean isPrimaryBean(AssertableApplicationContext context, String beanName) {
    return ((BeanDefinitionRegistry) context.getSourceApplicationContext()).getBeanDefinition(beanName).isPrimary();
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

  }

  @Configuration(proxyBeanMethods = false)
  static class UserDefinedValidatorConfig {

    @Bean
    OptionalValidatorFactoryBean customValidator() {
      return new OptionalValidatorFactoryBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class UserDefinedDefaultValidatorConfig {

    @Bean
    OptionalValidatorFactoryBean defaultValidator() {
      return new OptionalValidatorFactoryBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class UserDefinedJsrValidatorConfig {

    @Bean
    Validator customValidator() {
      return mock(Validator.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class UserDefinedInfraValidatorConfig {

    @Bean
    infra.validation.Validator customValidator() {
      return mock(infra.validation.Validator.class);
    }

    @Bean
    infra.validation.Validator anotherCustomValidator() {
      return mock(infra.validation.Validator.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class UserDefinedPrimaryInfraValidatorConfig {

    @Bean
    infra.validation.Validator customValidator() {
      return mock(infra.validation.Validator.class);
    }

    @Bean
    @Primary
    infra.validation.Validator anotherCustomValidator() {
      return mock(infra.validation.Validator.class);
    }

  }

  @Validated
  static class SampleService {

    void doSomething(@Size(min = 3, max = 10) String name) {
    }

  }

  @Configuration(proxyBeanMethods = false)
  static final class ExcludedServiceConfiguration {

    @Bean
    ExcludedService excludedService() {
      return new ExcludedService();
    }

    @Bean
    MethodValidationExcludeFilter exclusionFilter() {
      return (type) -> type.equals(ExcludedService.class);
    }

  }

  @Validated
  static final class ExcludedService {

    void doSomething(@Size(min = 3, max = 10) String name) {
    }

  }

  interface AnotherSampleService {

    void doSomething(@Min(42) Integer counter);

  }

  @Validated
  static class DefaultAnotherSampleService implements AnotherSampleService {

    @Override
    public void doSomething(Integer counter) {
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class AnotherSampleServiceConfiguration {

    @Bean
    AnotherSampleService anotherSampleService() {
      return new DefaultAnotherSampleService();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class SampleConfiguration {

    @Bean
    MethodValidationPostProcessor testMethodValidationPostProcessor() {
      return new MethodValidationPostProcessor();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomValidatorConfiguration {

    CustomValidatorConfiguration(SomeService someService) {

    }

    @Bean
    Validator customValidator() {
      return new CustomValidatorBean();
    }

    @Bean
    static TestBeanPostProcessor testBeanPostProcessor() {
      return new TestBeanPostProcessor();
    }

    @Configuration(proxyBeanMethods = false)
    static class SomeServiceConfiguration {

      @Bean
      SomeService someService() {
        return new SomeService();
      }

    }

    static class SomeService {

    }

    static class TestBeanPostProcessor implements InitializationBeanPostProcessor {

      private Set<String> postProcessed = new HashSet<>();

      @Override
      public Object postProcessAfterInitialization(Object bean, String name) {
        this.postProcessed.add(name);
        return bean;
      }

      @Override
      public Object postProcessBeforeInitialization(Object bean, String name) {
        return bean;
      }

    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ConfigurationCustomizersConfiguration {

    @Bean
    @Order(1)
    ValidationConfigurationCustomizer customizerOne() {
      return mock(ValidationConfigurationCustomizer.class);
    }

    @Bean
    @Order(0)
    ValidationConfigurationCustomizer customizerTwo() {
      return mock(ValidationConfigurationCustomizer.class);
    }

  }

}