/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context.junit4.aci.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Value;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.env.PropertySource;
import infra.mock.env.MockPropertySource;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit4.JUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify that a {@link PropertySource} can be set via a
 * custom {@link ApplicationContextInitializer} in the TestContext Framework.
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
