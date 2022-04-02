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

package cn.taketoday.test.context.junit4.aci.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Profile;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit4.JUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify that any {@link ApplicationContextInitializer
 * ApplicationContextInitializers} implementing
 * {@link cn.taketoday.core.Ordered Ordered} or marked with
 * {@link cn.taketoday.core.annotation.Order @Order} will be sorted
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
