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

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.context.annotation.AnnotationBeanNameGenerator;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.stereotype.Component;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Unit tests ensuring that configuration class bean names as expressed via @Configuration
 * or @Component 'value' attributes are indeed respected, and that customization of bean
 * naming through a BeanNameGenerator strategy works as expected.
 *
 * @author Chris Beams
 */
public class ConfigurationBeanNameTests {

  @Test
  public void registerOuterConfig() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(A.class);
    ctx.refresh();
    assertThat(ctx.containsBean("outer")).isTrue();
    assertThat(ctx.containsBean("imported")).isTrue();
    assertThat(ctx.containsBean("nested")).isTrue();
    assertThat(ctx.containsBean("nestedBean")).isTrue();
  }

  @Test
  public void registerNestedConfig() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(A.B.class);
    ctx.refresh();
    assertThat(ctx.containsBean("outer")).isFalse();
    assertThat(ctx.containsBean("imported")).isFalse();
    assertThat(ctx.containsBean("nested")).isTrue();
    assertThat(ctx.containsBean("nestedBean")).isTrue();
  }

  @Test
  public void registerOuterConfig_withBeanNameGenerator() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.setBeanNameGenerator(new AnnotationBeanNameGenerator() {
      @Override
      public String generateBeanName(
              BeanDefinition definition, BeanDefinitionRegistry registry) {
        return "custom-" + super.generateBeanName(definition, registry);
      }
    });
    ctx.register(A.class);
    ctx.refresh();
    assertThat(ctx.containsBean("custom-outer")).isTrue();
    assertThat(ctx.containsBean("custom-imported")).isTrue();
    assertThat(ctx.containsBean("custom-nested")).isTrue();
    assertThat(ctx.containsBean("nestedBean")).isTrue();
  }

  @Configuration("outer")
  @Import(C.class)
  static class A {
    @Component("nested")
    static class B {
      @Bean
      public String nestedBean() { return ""; }
    }
  }

  @Configuration("imported")
  static class C {
    @Bean
    public String s() { return "s"; }
  }
}
