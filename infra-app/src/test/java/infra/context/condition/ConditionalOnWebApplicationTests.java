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

package infra.context.condition;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import infra.annotation.ConditionalOnWebApplication;
import infra.annotation.ConditionalOnWebApplication.Type;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.http.server.reactive.HttpHandler;
import infra.web.server.reactive.ReactiveWebServerFactory;
import infra.web.server.reactive.context.AnnotationConfigReactiveWebApplicationContext;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ConditionalOnWebApplication @ConditionalOnWebApplication}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class ConditionalOnWebApplicationTests {

  private ConfigurableApplicationContext context;

  @AfterEach
  void closeContext() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
//  @Disabled
  void testWebApplicationWithReactiveContext() {
    AnnotationConfigReactiveWebApplicationContext context = new AnnotationConfigReactiveWebApplicationContext();
    context.register(AnyWebApplicationConfiguration.class, ReactiveWebApplicationConfiguration.class);
    context.refresh();
    this.context = context;
    assertThat(this.context.getBeansOfType(String.class)).containsExactly(entry("any", "any"),
            entry("reactive", "reactive"));
  }

  @Test
  void testNonWebApplication() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(AnyWebApplicationConfiguration.class, ReactiveWebApplicationConfiguration.class);
    ctx.refresh();
    this.context = ctx;
    assertThat(this.context.getBeansOfType(String.class)).isEmpty();
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnWebApplication
  static class AnyWebApplicationConfiguration {

    @Bean
    String any() {
      return "any";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnWebApplication(type = Type.REACTIVE)
  static class ReactiveWebApplicationConfiguration {

    @Bean
    String reactive() {
      return "reactive";
    }

    @Bean
    ReactiveWebServerFactory reactiveWebServerFactory() {
      return new MockReactiveWebServerFactory();
    }

    @Bean
    HttpHandler httpHandler() {
      return (request, response) -> Mono.empty();
    }

  }

}
