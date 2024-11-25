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

package infra.app.logging;

import org.junit.jupiter.api.Test;

import infra.app.logging.LoggingSystem;
import infra.app.logging.java.JavaLoggingSystem;
import infra.test.classpath.ClassPathExclusions;

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
