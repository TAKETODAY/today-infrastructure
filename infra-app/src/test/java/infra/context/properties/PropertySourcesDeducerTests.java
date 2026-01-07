/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.context.properties;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import infra.context.ApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.support.PropertySourcesPlaceholderConfigurer;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PropertySourcesDeducer}.
 *
 * @author Phillip Webb
 */
class PropertySourcesDeducerTests {

  @Test
  void getPropertySourcesWhenHasSinglePropertySourcesPlaceholderConfigurerReturnsBean() {
    ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
            PropertySourcesPlaceholderConfigurerConfiguration.class);
    PropertySourcesDeducer deducer = new PropertySourcesDeducer(applicationContext);
    PropertySources propertySources = deducer.getPropertySources();
    assertThat(propertySources.get("test")).isInstanceOf(TestPropertySource.class);
  }

  @Test
  void getPropertySourcesWhenHasNoPropertySourcesPlaceholderConfigurerReturnsEnvironmentSources() {
    ApplicationContext applicationContext = new AnnotationConfigApplicationContext(EmptyConfiguration.class);
    ConfigurableEnvironment environment = (ConfigurableEnvironment) applicationContext.getEnvironment();
    environment.getPropertySources().addFirst(new TestPropertySource());
    PropertySourcesDeducer deducer = new PropertySourcesDeducer(applicationContext);
    PropertySources propertySources = deducer.getPropertySources();
    assertThat(propertySources.get("test")).isInstanceOf(TestPropertySource.class);
  }

  @Test
  void getPropertySourcesWhenHasMultiplePropertySourcesPlaceholderConfigurerReturnsEnvironmentSources() {
    ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
            MultiplePropertySourcesPlaceholderConfigurerConfiguration.class);
    ConfigurableEnvironment environment = (ConfigurableEnvironment) applicationContext.getEnvironment();
    environment.getPropertySources().addFirst(new TestPropertySource());
    PropertySourcesDeducer deducer = new PropertySourcesDeducer(applicationContext);
    PropertySources propertySources = deducer.getPropertySources();
    assertThat(propertySources.get("test")).isInstanceOf(TestPropertySource.class);
  }

  @Test
  void getPropertySourcesWhenUnavailableThrowsException() {
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    Environment environment = mock(Environment.class);
    given(applicationContext.getEnvironment()).willReturn(environment);
    PropertySourcesDeducer deducer = new PropertySourcesDeducer(applicationContext);
    assertThatIllegalStateException().isThrownBy(() -> deducer.getPropertySources()).withMessage(
            "Unable to obtain PropertySources from PropertySourcesPlaceholderConfigurer or Environment");
  }

  @Configuration(proxyBeanMethods = false)
  static class PropertySourcesPlaceholderConfigurerConfiguration {

    @Bean
    PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
      PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
      PropertySources propertySources = new PropertySources();
      propertySources.addFirst(new TestPropertySource());
      configurer.setPropertySources(propertySources);
      return configurer;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class EmptyConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  static class MultiplePropertySourcesPlaceholderConfigurerConfiguration {

    @Bean
    PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer1() {
      return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer2() {
      return new PropertySourcesPlaceholderConfigurer();
    }

  }

  private static class TestPropertySource extends MapPropertySource {

    TestPropertySource() {
      super("test", Collections.emptyMap());
    }

  }

}
