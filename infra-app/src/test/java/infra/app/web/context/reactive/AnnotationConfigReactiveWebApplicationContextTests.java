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

package infra.app.web.context.reactive;

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