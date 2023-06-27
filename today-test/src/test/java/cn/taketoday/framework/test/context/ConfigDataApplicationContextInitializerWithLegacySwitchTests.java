/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.env.Environment;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigDataApplicationContextInitializer}.
 *
 * @author Phillip Webb
 */
@ExtendWith(InfraExtension.class)
@DirtiesContext
@TestPropertySource(properties = "app.config.use-legacy-processing=true")
@ContextConfiguration(classes = ConfigDataApplicationContextInitializerWithLegacySwitchTests.Config.class,
                      initializers = ConfigDataApplicationContextInitializer.class)
class ConfigDataApplicationContextInitializerWithLegacySwitchTests {

  @Autowired
  private Environment environment;

  @Test
  void initializerPopulatesEnvironment() {
    assertThat(this.environment.getProperty("foo")).isEqualTo("bucket");
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

  }

}
