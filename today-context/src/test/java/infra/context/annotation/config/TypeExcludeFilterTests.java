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

package infra.context.annotation.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.ComponentScan;
import infra.context.annotation.ComponentScan.Filter;
import infra.context.annotation.Configuration;
import infra.context.annotation.FilterType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 23:48
 */
class TypeExcludeFilterTests {

  private AnnotationConfigApplicationContext context;

  @AfterEach
  void cleanUp() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void loadsTypeExcludeFilters() {
    this.context = new AnnotationConfigApplicationContext();
    this.context.getBeanFactory().registerSingleton("filter1", new WithoutMatchOverrideFilter());
    this.context.getBeanFactory().registerSingleton("filter2", new SampleTypeExcludeFilter());
    this.context.register(Config.class);
    this.context.refresh();
    assertThat(this.context.getBean(ExampleComponent.class)).isNotNull();
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> this.context.getBean(ExampleFilteredComponent.class));
  }

  @Configuration(proxyBeanMethods = false)
  @ComponentScan(basePackageClasses = SampleTypeExcludeFilter.class,
                 excludeFilters = @Filter(type = FilterType.CUSTOM, classes = SampleTypeExcludeFilter.class))
  static class Config {

  }

  static class WithoutMatchOverrideFilter extends TypeExcludeFilter {

  }

}
