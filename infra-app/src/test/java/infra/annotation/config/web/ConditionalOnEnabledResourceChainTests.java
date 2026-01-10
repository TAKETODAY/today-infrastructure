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

package infra.annotation.config.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnEnabledResourceChain @ConditionalOnEnabledResourceChain}.
 *
 * @author Stephane Nicoll
 */
class ConditionalOnEnabledResourceChainTests {

  private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

  @AfterEach
  void closeContext() {
    this.context.close();
  }

  @Test
  void disabledByDefault() {
    load();
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void disabledExplicitly() {
    load("web.resources.chain.enabled:false");
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void enabledViaMainEnabledFlag() {
    load("web.resources.chain.enabled:true");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void enabledViaFixedStrategyFlag() {
    load("web.resources.chain.strategy.fixed.enabled:true");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void enabledViaContentStrategyFlag() {
    load("web.resources.chain.strategy.content.enabled:true");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  private void load(String... environment) {
    this.context.register(Config.class);
    TestPropertyValues.of(environment).applyTo(this.context);
    this.context.refresh();
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    @ConditionalOnEnabledResourceChain
    String foo() {
      return "foo";
    }

  }

}
