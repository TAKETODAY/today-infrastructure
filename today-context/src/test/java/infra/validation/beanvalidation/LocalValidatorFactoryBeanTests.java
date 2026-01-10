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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import infra.context.MessageSource;
import infra.core.ParameterNameDiscoverer;
import infra.core.io.ClassPathResource;
import jakarta.validation.ClockProvider;
import jakarta.validation.Configuration;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.TraversableResolver;
import jakarta.validation.ValidationException;
import jakarta.validation.ValidatorContext;
import jakarta.validation.ValidatorFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/5/8 10:55
 */
class LocalValidatorFactoryBeanTests {

  @Test
  void defaultFactoryConfiguration() {
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.afterPropertiesSet();

    assertThat(factory.getValidator()).isNotNull();
    assertThat(factory.getMessageInterpolator()).isNotNull();
    assertThat(factory.getTraversableResolver()).isNotNull();
    assertThat(factory.getConstraintValidatorFactory()).isNotNull();
  }

  @Test
  void customMessageInterpolatorConfiguration() {
    MessageInterpolator customInterpolator = mock(MessageInterpolator.class);
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.setMessageInterpolator(customInterpolator);
    factory.afterPropertiesSet();

    MessageInterpolator resultInterpolator = factory.getMessageInterpolator();
    assertThat(resultInterpolator).isInstanceOf(LocaleContextMessageInterpolator.class);
  }

  @Test
  void customMessageSourceConfiguration() {
    MessageSource messageSource = mock(MessageSource.class);
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.setValidationMessageSource(messageSource);
    factory.afterPropertiesSet();

    assertThat(factory.getMessageInterpolator())
            .isInstanceOf(LocaleContextMessageInterpolator.class);
  }

  @Test
  void customValidationPropertyConfiguration() {
    Map<String, String> properties = new HashMap<>();
    properties.put("hibernate.validator.fail_fast", "true");

    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.setValidationPropertyMap(properties);
    factory.afterPropertiesSet();

    assertThat(factory.getValidationPropertyMap())
            .containsEntry("hibernate.validator.fail_fast", "true");
  }

  @Test
  void invalidMappingLocationThrowsException() {
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.setMappingLocations(new ClassPathResource("invalid.xml"));

    assertThatIllegalStateException()
            .isThrownBy(factory::afterPropertiesSet)
            .withMessageContaining("Cannot read mapping resource");
  }

  @Test
  void customConstraintValidatorFactoryConfiguration() {
    ConstraintValidatorFactory customFactory = mock(ConstraintValidatorFactory.class);
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.setConstraintValidatorFactory(customFactory);
    factory.afterPropertiesSet();

    assertThat(factory.getConstraintValidatorFactory()).isSameAs(customFactory);
  }

  @Test
  void configurationInitializerIsCalled() {
    AtomicBoolean initializerCalled = new AtomicBoolean(false);
    Consumer<Configuration<?>> initializer = configuration -> initializerCalled.set(true);

    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.setConfigurationInitializer(initializer);
    factory.afterPropertiesSet();

    assertThat(initializerCalled).isTrue();
  }

  @Test
  void unwrapToValidatorFactoryReturnsInstance() {
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.afterPropertiesSet();

    ValidatorFactory unwrapped = factory.unwrap(ValidatorFactory.class);
    assertThat(unwrapped).isNotNull();
  }

  @Test
  void unwrapToUnsupportedTypeThrowsException() {
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.afterPropertiesSet();

    assertThatExceptionOfType(ValidationException.class)
            .isThrownBy(() -> factory.unwrap(String.class))
            .withMessageContaining("Type java.lang.String not supported for unwrapping");
  }

  @Test
  void closeDestroyingValidatorFactory() {
    ValidatorFactory mockValidatorFactory = mock(ValidatorFactory.class);
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean() {
      @Override
      public void afterPropertiesSet() {
        this.validatorFactory = mockValidatorFactory;
      }
    };
    factory.afterPropertiesSet();
    factory.close();
    factory.destroy();

    verify(mockValidatorFactory, times(2)).close();
  }

  @Test
  void customTraversableResolverConfiguration() {
    TraversableResolver customResolver = mock(TraversableResolver.class);
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.setTraversableResolver(customResolver);
    factory.afterPropertiesSet();

    TraversableResolver resultResolver = factory.getTraversableResolver();
    assertThat(resultResolver).isSameAs(customResolver);
  }

  @Test
  void validationPropertiesFromProperties() {
    Properties properties = new Properties();
    properties.setProperty("key1", "value1");
    properties.setProperty("key2", "value2");

    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.setValidationProperties(properties);
    factory.afterPropertiesSet();

    assertThat(factory.getValidationPropertyMap())
            .containsEntry("key1", "value1")
            .containsEntry("key2", "value2");
  }

  @Test
  void usingContextDelegatesToValidatorFactory() {
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.afterPropertiesSet();

    ValidatorContext context = factory.usingContext();
    assertThat(context).isNotNull();
  }

  @Test
  void getClockProviderDelegatesToValidatorFactory() {
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.afterPropertiesSet();

    ClockProvider provider = factory.getClockProvider();
    assertThat(provider).isNotNull();
  }

  @Test
  void getParameterNameProviderDelegatesToValidatorFactory() {
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.afterPropertiesSet();

    ParameterNameProvider provider = factory.getParameterNameProvider();
    assertThat(provider).isNotNull();
  }

  @Test
  void customParameterNameDiscovererConfiguration() {
    ParameterNameDiscoverer customDiscoverer = mock(ParameterNameDiscoverer.class);
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.setParameterNameDiscoverer(customDiscoverer);
    factory.afterPropertiesSet();

    ParameterNameProvider provider = factory.getParameterNameProvider();
    assertThat(provider).isNotNull();
  }

}