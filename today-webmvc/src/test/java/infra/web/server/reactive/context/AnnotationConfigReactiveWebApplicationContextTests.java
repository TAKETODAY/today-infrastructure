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

package infra.web.server.reactive.context;

import org.junit.jupiter.api.Test;

import infra.beans.factory.support.StandardBeanFactory;
import infra.context.annotation.Configuration;
import infra.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:21
 */
class AnnotationConfigReactiveWebApplicationContextTests {

  @Test
  void shouldCreateContextWithBeanFactory() {
    // given
    StandardBeanFactory beanFactory = new StandardBeanFactory();

    // when
    AnnotationConfigReactiveWebApplicationContext context = new AnnotationConfigReactiveWebApplicationContext(beanFactory);

    // then
    assertThat(context).isNotNull();
    assertThat(context.getBeanFactory()).isEqualTo(beanFactory);
  }

  @Test
  void shouldCreateContextWithAnnotatedClasses() {
    // when
    AnnotationConfigReactiveWebApplicationContext context = new AnnotationConfigReactiveWebApplicationContext(Config.class);

    // then
    assertThat(context).isNotNull();
    assertThat(context.isActive()).isTrue();
  }

  @Test
  void shouldCreateStandardReactiveWebEnvironment() {
    // given
    AnnotationConfigReactiveWebApplicationContext context = new AnnotationConfigReactiveWebApplicationContext();

    // when
    ConfigurableEnvironment environment = context.createEnvironment();

    // then
    assertThat(environment).isNotNull();
    assertThat(environment).isInstanceOf(StandardReactiveWebEnvironment.class);
  }

  @Configuration
  static class Config {

  }

}