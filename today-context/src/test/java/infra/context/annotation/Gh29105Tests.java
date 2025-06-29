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

package infra.context.annotation;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import infra.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for gh-29105.
 *
 * @author Stephane Nicoll
 */
public class Gh29105Tests {

  @Test
  void beanProviderWithParentContextReuseOrder() {
    AnnotationConfigApplicationContext parent =
            new AnnotationConfigApplicationContext(DefaultConfiguration.class, CustomConfiguration.class);

    AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
    child.setParent(parent);
    child.register(DefaultConfiguration.class);
    child.refresh();

    Stream<Class<?>> orderedTypes = child.getBeanProvider(MyService.class).orderedStream().map(Object::getClass);
    assertThat(orderedTypes).containsExactly(CustomService.class, DefaultService.class);

    assertThat(child.getBeanFactory().getOrder("defaultService")).isEqualTo(0);
    assertThat(child.getBeanFactory().getOrder("customService")).isEqualTo(-1);

    child.close();
    parent.close();
  }

  interface MyService { }

  static class CustomService implements MyService { }

  static class DefaultService implements MyService { }

  @Configuration
  static class CustomConfiguration {

    @Bean
    @Order(-1)
    CustomService customService() {
      return new CustomService();
    }

  }

  @Configuration
  static class DefaultConfiguration {

    @Bean
    @Order(0)
    DefaultService defaultService() {
      return new DefaultService();
    }

  }

}
