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

import java.util.Collection;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.ImportResource;
import cn.taketoday.framework.test.context.assertj.AssertableApplicationContext;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnClass @ConditionalOnClass}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class ConditionalOnClassTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void testVanillaOnClassCondition() {
    this.contextRunner.withUserConfiguration(BasicConfiguration.class, FooConfiguration.class)
            .run(this::hasBarBean);
  }

  @Test
  void testMissingOnClassCondition() {
    this.contextRunner.withUserConfiguration(MissingConfiguration.class, FooConfiguration.class).run((context) -> {
      assertThat(context).doesNotHaveBean("bar");
      assertThat(context).hasBean("foo");
      assertThat(context.getBean("foo")).isEqualTo("foo");
    });
  }

  @Test
  void testOnClassConditionWithXml() {
    this.contextRunner.withUserConfiguration(BasicConfiguration.class, XmlConfiguration.class)
            .run(this::hasBarBean);
  }

  @Test
  void testOnClassConditionWithCombinedXml() {
    this.contextRunner.withUserConfiguration(CombinedXmlConfiguration.class).run(this::hasBarBean);
  }

  @Test
  void onClassConditionOutputShouldNotContainConditionalOnMissingClassInMessage() {
    this.contextRunner.withUserConfiguration(BasicConfiguration.class).run((context) -> {
      Collection<ConditionEvaluationReport.ConditionAndOutcomes> conditionAndOutcomes = ConditionEvaluationReport
              .get(context.getSourceApplicationContext().getBeanFactory()).getConditionAndOutcomesBySource()
              .values();
      String message = conditionAndOutcomes.iterator().next().iterator().next().getOutcome().getMessage();
      assertThat(message).doesNotContain("@ConditionalOnMissingClass did not find unwanted class");
    });
  }

  private void hasBarBean(AssertableApplicationContext context) {
    assertThat(context).hasBean("bar");
    assertThat(context.getBean("bar")).isEqualTo("bar");
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(ConditionalOnClassTests.class)
  static class BasicConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(name = "FOO")
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

  @Configuration(proxyBeanMethods = false)
  @ImportResource("cn/taketoday/framework/autoconfigure/condition/foo.xml")
  static class XmlConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @Import(BasicConfiguration.class)
  @ImportResource("cn/taketoday/framework/autoconfigure/condition/foo.xml")
  static class CombinedXmlConfiguration {

  }

}
