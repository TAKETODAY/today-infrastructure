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
import cn.taketoday.context.annotation.ConditionEvaluationContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ContextCondition}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
class ContextConditionTests {

  @Test
  void sensibleClassException() {
    assertThatIllegalStateException().isThrownBy(() -> new AnnotationConfigApplicationContext(ErrorOnClass.class))
            .withMessageContaining("Error processing condition on " + ErrorOnClass.class.getName());
  }

  @Test
  void sensibleMethodException() {
    assertThatIllegalStateException().isThrownBy(() -> new AnnotationConfigApplicationContext(ErrorOnMethod.class))
            .withMessageContaining("Error processing condition on " + ErrorOnMethod.class.getName() + ".myBean");
  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(AlwaysThrowsCondition.class)
  static class ErrorOnClass {

  }

  @Configuration(proxyBeanMethods = false)
  static class ErrorOnMethod {

    @Bean
    @Conditional(AlwaysThrowsCondition.class)
    String myBean() {
      return "bean";
    }

  }

  static class AlwaysThrowsCondition extends ContextCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
      throw new RuntimeException("Oh no!");
    }

  }

}
