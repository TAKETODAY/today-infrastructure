/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.util.TestPropertyValues;
import cn.taketoday.web.config.ConditionalOnEnabledResourceChain;

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
