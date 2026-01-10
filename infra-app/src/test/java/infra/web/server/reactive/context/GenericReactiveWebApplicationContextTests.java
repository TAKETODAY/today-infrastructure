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
