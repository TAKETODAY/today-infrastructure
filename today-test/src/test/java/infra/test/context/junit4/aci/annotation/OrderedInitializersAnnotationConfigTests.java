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

package infra.test.context.junit4.aci.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Profile;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit4.JUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify that any {@link ApplicationContextInitializer
 * ApplicationContextInitializers} implementing
 * {@link Ordered Ordered} or marked with
 * {@link Order @Order} will be sorted
 * appropriately in conjunction with annotation-driven configuration in the
 * TestContext framework.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
// Note: the ordering of the config classes is intentionally: global, two, one.
// Note: the ordering of the initializers is intentionally: two, one.
@ContextConfiguration(
        classes = { OrderedInitializersAnnotationConfigTests.GlobalConfig.class, OrderedInitializersAnnotationConfigTests.ConfigTwo.class, OrderedInitializersAnnotationConfigTests.ConfigOne.class },
        initializers = {
                OrderedInitializersAnnotationConfigTests.OrderedTwoInitializer.class, OrderedInitializersAnnotationConfigTests.OrderedOneInitializer.class })
public class OrderedInitializersAnnotationConfigTests {

  private static final String PROFILE_GLOBAL = "global";
  private static final String PROFILE_ONE = "one";
  private static final String PROFILE_TWO = "two";

  @Autowired
  private String foo, bar, baz;

  @Test
  public void activeBeans() {
    assertThat(foo).isEqualTo(PROFILE_GLOBAL);
    assertThat(bar).isEqualTo(PROFILE_GLOBAL);
    assertThat(baz).isEqualTo(PROFILE_TWO);
  }

  // -------------------------------------------------------------------------

  @Configuration
  static class GlobalConfig {

    @Bean
    public String foo() {
      return PROFILE_GLOBAL;
    }

    @Bean
    public String bar() {
      return PROFILE_GLOBAL;
    }

    @Bean
    public String baz() {
      return PROFILE_GLOBAL;
    }
  }

  @Configuration
  @Profile(PROFILE_ONE)
  static class ConfigOne {

    @Bean
    public String foo() {
      return PROFILE_ONE;
    }

    @Bean
    public String bar() {
      return PROFILE_ONE;
    }

    @Bean
    public String baz() {
      return PROFILE_ONE;
    }
  }

  @Configuration
  @Profile(PROFILE_TWO)
  static class ConfigTwo {

    @Bean
    public String baz() {
      return PROFILE_TWO;
    }
  }

  // -------------------------------------------------------------------------

  static class OrderedOneInitializer implements ApplicationContextInitializer, Ordered {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      applicationContext.getEnvironment().setActiveProfiles(PROFILE_ONE);
    }

    @Override
    public int getOrder() {
      return 1;
    }
  }

  @Order(2)
  static class OrderedTwoInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      applicationContext.getEnvironment().setActiveProfiles(PROFILE_TWO);
    }
  }

}
