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
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.mock.env.MockPropertySource;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit4.JUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify that a {@link PropertySource} can be set via a
 * custom {@link ApplicationContextInitializer} in the Spring TestContext Framework.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@ContextConfiguration(initializers = PropertySourcesInitializerTests.PropertySourceInitializer.class)
public class PropertySourcesInitializerTests {

  @Configuration
  static class Config {

    @Value("${enigma}")
    // The following can also be used to directly access the
    // environment instead of relying on placeholder replacement.
    // @Value("#{ environment['enigma'] }")
    private String enigma;

    @Bean
    public String enigma() {
      return enigma;
    }

  }

  @Autowired
  private String enigma;

  @Test
  public void customPropertySourceConfiguredViaContextInitializer() {
    assertThat(enigma).isEqualTo("foo");
  }

  public static class PropertySourceInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      applicationContext.getEnvironment().getPropertySources().addFirst(
              new MockPropertySource().withProperty("enigma", "foo"));
    }
  }

}
