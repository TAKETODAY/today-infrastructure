/*
 * Copyright 2002-present the original author or authors.
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
