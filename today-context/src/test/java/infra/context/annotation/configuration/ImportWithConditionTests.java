/*
 * Copyright 2017 - 2023 the original author or authors.
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

package infra.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.context.annotation.ConfigurationCondition;
import infra.context.annotation.Import;
import infra.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Andy Wilkinson
 */
public class ImportWithConditionTests {

  private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

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
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return false;
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
      return ConfigurationPhase.REGISTER_BEAN;
    }
  }

}
