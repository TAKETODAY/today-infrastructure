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

package cn.taketoday.context.condition;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnMissingClass @ConditionalOnMissingClass}.
 *
 * @author Dave Syer
 */
class ConditionalOnMissingClassTests {

  private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

  @Test
  void testVanillaOnClassCondition() {
    this.context.register(BasicConfiguration.class, FooConfiguration.class);
    this.context.refresh();
    assertThat(this.context.containsBean("bar")).isFalse();
    assertThat(this.context.getBean("foo")).isEqualTo("foo");
  }

  @Test
  void testMissingOnClassCondition() {
    this.context.register(MissingConfiguration.class, FooConfiguration.class);
    this.context.refresh();
    assertThat(this.context.containsBean("bar")).isTrue();
    assertThat(this.context.getBean("foo")).isEqualTo("foo");
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingClass("cn.taketoday.context.condition.ConditionalOnMissingClassTests")
  static class BasicConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingClass("FOO")
  static class MissingConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class FooConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

}
