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
import infra.core.env.ConfigurableEnvironment;
import infra.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GenericReactiveWebApplicationContext}
 *
 * @author Brian Clozel
 */
class GenericReactiveWebApplicationContextTests {

  @Test
  void getResourceByPath() throws Exception {
    GenericReactiveWebApplicationContext context = new GenericReactiveWebApplicationContext();
    Resource rootResource = context.getResourceByPath("/");
    assertThat(rootResource.exists()).isFalse();
    assertThat(rootResource.createRelative("application.properties").exists()).isFalse();
    context.close();
  }

  @Test
  void shouldCreateEmptyContext() {
    // when
    GenericReactiveWebApplicationContext context = new GenericReactiveWebApplicationContext();

    // then
    assertThat(context).isNotNull();
    context.close();
  }

  @Test
  void shouldCreateContextWithBeanFactory() {
    // given
    StandardBeanFactory beanFactory = new StandardBeanFactory();

    // when
    GenericReactiveWebApplicationContext context = new GenericReactiveWebApplicationContext(beanFactory);

    // then
    assertThat(context).isNotNull();
    assertThat(context.getBeanFactory()).isEqualTo(beanFactory);
    context.close();
  }

  @Test
  void shouldCreateStandardReactiveWebEnvironment() {
    // given
    GenericReactiveWebApplicationContext context = new GenericReactiveWebApplicationContext();

    // when
    ConfigurableEnvironment environment = context.getEnvironment();

    // then
    assertThat(environment).isNotNull();
    assertThat(environment).isInstanceOf(StandardReactiveWebEnvironment.class);
    context.close();
  }

  @Test
  void shouldReturnFilteredReactiveWebContextResource() {
    // given
    GenericReactiveWebApplicationContext context = new GenericReactiveWebApplicationContext();
    String path = "test/resource";

    // when
    Resource resource = context.getResourceByPath(path);

    // then
    assertThat(resource).isNotNull();
    assertThat(resource).isInstanceOf(FilteredReactiveWebContextResource.class);
    context.close();
  }

}
