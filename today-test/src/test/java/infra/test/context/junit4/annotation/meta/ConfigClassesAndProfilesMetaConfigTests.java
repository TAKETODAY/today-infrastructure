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

package infra.test.context.junit4.annotation.meta;

import org.junit.Test;
import org.junit.runner.RunWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Profile;
import infra.test.context.junit4.JUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for meta-annotation attribute override support, demonstrating
 * that the test class is used as the <em>declaring class</em> when detecting default
 * configuration classes for the declaration of {@code @ContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@ConfigClassesAndProfilesMetaConfig(profiles = "dev")
public class ConfigClassesAndProfilesMetaConfigTests {

  @Configuration
  @Profile("dev")
  static class DevConfig {

    @Bean
    public String foo() {
      return "Local Dev Foo";
    }
  }

  @Configuration
  @Profile("prod")
  static class ProductionConfig {

    @Bean
    public String foo() {
      return "Local Production Foo";
    }
  }

  @Autowired
  private String foo;

  @Test
  public void foo() {
    assertThat(foo).isEqualTo("Local Dev Foo");
  }
}
