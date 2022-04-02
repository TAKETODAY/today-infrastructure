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

package cn.taketoday.test.context.junit4.annotation.meta;

import org.junit.Test;
import org.junit.runner.RunWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Profile;
import cn.taketoday.test.context.junit4.JUnit4ClassRunner;

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
