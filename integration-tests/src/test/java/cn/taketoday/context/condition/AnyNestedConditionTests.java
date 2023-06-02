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

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.framework.test.context.assertj.AssertableApplicationContext;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.test.context.runner.ContextConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnyNestedCondition}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
class AnyNestedConditionTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void neither() {
    this.contextRunner.withUserConfiguration(Config.class).run(match(false));
  }

  @Test
  void propertyA() {
    this.contextRunner.withUserConfiguration(Config.class).withPropertyValues("a:a").run(match(true));
  }

  @Test
  void propertyB() {
    this.contextRunner.withUserConfiguration(Config.class).withPropertyValues("b:b").run(match(true));
  }

  @Test
  void both() {
    this.contextRunner.withUserConfiguration(Config.class).withPropertyValues("a:a", "b:b").run(match(true));
  }

  private ContextConsumer<AssertableApplicationContext> match(boolean expected) {
    return (context) -> {
      if (expected) {
        assertThat(context).hasBean("myBean");
      }
      else {
        assertThat(context).doesNotHaveBean("myBean");
      }
    };
  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(OnPropertyAorBCondition.class)
  static class Config {

    @Bean
    String myBean() {
      return "myBean";
    }

  }

  static class OnPropertyAorBCondition extends AnyNestedCondition {

    OnPropertyAorBCondition() {
      super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty("a")
    static class HasPropertyA {

    }

    @ConditionalOnExpression("true")
    @ConditionalOnProperty("b")
    static class HasPropertyB {

    }

    @Conditional(NonSpringBootCondition.class)
    static class SubclassC {

    }

  }

  static class NonSpringBootCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return false;
    }

  }

}
