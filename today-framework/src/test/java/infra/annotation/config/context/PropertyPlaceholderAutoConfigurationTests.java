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

package infra.annotation.config.context;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Value;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.context.support.PropertySourcesPlaceholderConfigurer;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertyPlaceholderAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class PropertyPlaceholderAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void whenTheAutoConfigurationIsNotUsedThenBeanDefinitionPlaceholdersAreNotResolved() {
    this.contextRunner.withPropertyValues("fruit:banana").withInitializer(this::definePlaceholderBean)
            .run((context) -> assertThat(context.getBean(PlaceholderBean.class).fruit).isEqualTo("${fruit:apple}"));
  }

  @Test
  void whenTheAutoConfigurationIsUsedThenBeanDefinitionPlaceholdersAreResolved() {
    this.contextRunner.withPropertyValues("fruit:banana").withInitializer(this::definePlaceholderBean)
            .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
            .run((context) -> assertThat(context.getBean(PlaceholderBean.class).fruit).isEqualTo("banana"));
  }

  @Test
  void whenTheAutoConfigurationIsNotUsedThenValuePlaceholdersAreResolved() {
    this.contextRunner.withPropertyValues("fruit:banana").withUserConfiguration(PlaceholderConfig.class)
            .run((context) -> assertThat(context.getBean(PlaceholderConfig.class).fruit).isEqualTo("banana"));
  }

  @Test
  void whenTheAutoConfigurationIsUsedThenValuePlaceholdersAreResolved() {
    this.contextRunner.withPropertyValues("fruit:banana")
            .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(PlaceholderConfig.class)
            .run((context) -> assertThat(context.getBean(PlaceholderConfig.class).fruit).isEqualTo("banana"));
  }

  @Test
  void whenThereIsAUserDefinedPropertySourcesPlaceholderConfigurerThenItIsUsedForBeanDefinitionPlaceholderResolution() {
    this.contextRunner.withPropertyValues("fruit:banana").withInitializer(this::definePlaceholderBean)
            .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(PlaceholdersOverride.class)
            .run((context) -> assertThat(context.getBean(PlaceholderBean.class).fruit).isEqualTo("orange"));
  }

  @Test
  void whenThereIsAUserDefinedPropertySourcesPlaceholderConfigurerThenItIsUsedForValuePlaceholderResolution() {
    this.contextRunner.withPropertyValues("fruit:banana")
            .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(PlaceholderConfig.class, PlaceholdersOverride.class)
            .run((context) -> assertThat(context.getBean(PlaceholderConfig.class).fruit).isEqualTo("orange"));
  }

  private void definePlaceholderBean(ConfigurableApplicationContext context) {
    ((BeanDefinitionRegistry) context.getBeanFactory()).registerBeanDefinition("placeholderBean",
            BeanDefinitionBuilder.rootBeanDefinition(PlaceholderBean.class).addConstructorArgValue("${fruit:apple}")
                    .getBeanDefinition());
  }

  @Configuration(proxyBeanMethods = false)
  static class PlaceholderConfig {

    @Value("${fruit:apple}")
    private String fruit;

  }

  static class PlaceholderBean {

    private final String fruit;

    PlaceholderBean(String fruit) {
      this.fruit = fruit;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class PlaceholdersOverride {

    @Bean
    static PropertySourcesPlaceholderConfigurer morePlaceholders() {
      PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
      configurer
              .setProperties(StringUtils.splitArrayElementsIntoProperties(new String[] { "fruit=orange" }, "="));
      configurer.setLocalOverride(true);
      configurer.setOrder(0);
      return configurer;
    }

  }

}
