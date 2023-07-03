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

package cn.taketoday.framework.logging;

import org.junit.jupiter.api.Test;

import cn.taketoday.framework.logging.java.JavaLoggingSystem;
import cn.taketoday.test.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoggingSystem} when Logback is not on the classpath.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions({ "logback-*.jar" })
class LogbackAndLog4J2ExcludedLoggingSystemTests {

  @Test
  void whenLogbackAndLog4J2AreNotPresentJULIsTheLoggingSystem() {
    assertThat(LoggingSystem.get(getClass().getClassLoader())).isInstanceOf(JavaLoggingSystem.class);
  }

}
