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

package cn.taketoday.context.condition;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication.Type;
import cn.taketoday.web.server.reactive.context.AnnotationConfigReactiveWebApplicationContext;
import cn.taketoday.web.server.reactive.ReactiveWebServerFactory;
import cn.taketoday.http.server.reactive.HttpHandler;
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
