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

package infra.context.condition;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ResourceCondition}.
 *
 * @author Stephane Nicoll
 */
class ResourceConditionTests {

  private ConfigurableApplicationContext context;

  @AfterEach
  void tearDown() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void defaultResourceAndNoExplicitKey() {
    load(DefaultLocationConfiguration.class);
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void unknownDefaultLocationAndNoExplicitKey() {
    load(UnknownDefaultLocationConfiguration.class);
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void unknownDefaultLocationAndExplicitKeyToResource() {
    load(UnknownDefaultLocationConfiguration.class, "spring.foo.test.config=logging.properties");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  private void load(Class<?> config, String... environment) {
    AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
    TestPropertyValues.of(environment).applyTo(applicationContext);
    applicationContext.register(config);
    applicationContext.refresh();
    this.context = applicationContext;
  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(DefaultLocationResourceCondition.class)
  static class DefaultLocationConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(UnknownDefaultLocationResourceCondition.class)
  static class UnknownDefaultLocationConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

  static class DefaultLocationResourceCondition extends ResourceCondition {

    DefaultLocationResourceCondition() {
      super("test", "spring.foo.test.config", "classpath:/logging-nondefault.properties");
    }

  }

  static class UnknownDefaultLocationResourceCondition extends ResourceCondition {

    UnknownDefaultLocationResourceCondition() {
      super("test", "spring.foo.test.config", "classpath:/this-file-does-not-exist.xml");
    }

  }

}
