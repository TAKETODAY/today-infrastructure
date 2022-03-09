/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.ConditionEvaluationContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ConfigurationCondition;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Andy Wilkinson
 */
public class ImportWithConditionTests {

  private StandardApplicationContext context = new StandardApplicationContext();

  @Test
  public void conditionalThenUnconditional() throws Exception {
    this.context.register(ConditionalThenUnconditional.class);
    this.context.refresh();
    assertThat(this.context.containsBean("beanTwo")).isFalse();
    assertThat(this.context.containsBean("beanOne")).isTrue();
  }

  @Test
  public void unconditionalThenConditional() throws Exception {
    this.context.register(UnconditionalThenConditional.class);
    this.context.refresh();
    assertThat(this.context.containsBean("beanTwo")).isFalse();
    assertThat(this.context.containsBean("beanOne")).isTrue();
  }

  @Configuration
  @Import({ ConditionalConfiguration.class, UnconditionalConfiguration.class })
  protected static class ConditionalThenUnconditional {

    @Autowired
    @SuppressWarnings("unused")
    private BeanOne beanOne;
  }

  @Configuration
  @Import({ UnconditionalConfiguration.class, ConditionalConfiguration.class })
  protected static class UnconditionalThenConditional {

    @Autowired
    @SuppressWarnings("unused")
    private BeanOne beanOne;
  }

  @Configuration
  @Import(BeanProvidingConfiguration.class)
  protected static class UnconditionalConfiguration {
  }

  @Configuration
  @Conditional(NeverMatchingCondition.class)
  @Import(BeanProvidingConfiguration.class)
  protected static class ConditionalConfiguration {
  }

  @Configuration
  protected static class BeanProvidingConfiguration {

    @Bean
    BeanOne beanOne() {
      return new BeanOne();
    }
  }

  private static final class BeanOne {
  }

  private static final class NeverMatchingCondition implements ConfigurationCondition {

    @Override
    public boolean matches(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
      return false;
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
      return ConfigurationPhase.REGISTER_BEAN;
    }
  }

}
