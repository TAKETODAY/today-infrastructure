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

package cn.taketoday.test.context.env;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.env.Environment;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify detection of a default properties file
 * when {@link TestPropertySource @TestPropertySource} is <em>empty</em>.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ExtendWith(ApplicationExtension.class)
@ContextConfiguration
@TestPropertySource
class DefaultPropertiesFileDetectionTestPropertySourceTests {

  @Autowired
  protected Environment env;

  @Test
  void verifyPropertiesAreAvailableInEnvironment() {
    // from DefaultPropertiesFileDetectionTestPropertySourceTests.properties
    assertEnvironmentValue("riddle", "auto detected");
  }

  protected void assertEnvironmentValue(String key, String expected) {
    assertThat(env.getProperty(key)).as("Value of key [" + key + "].").isEqualTo(expected);
  }

  // -------------------------------------------------------------------

  @Configuration
  static class Config {
    /* no user beans required for these tests */
  }

}
