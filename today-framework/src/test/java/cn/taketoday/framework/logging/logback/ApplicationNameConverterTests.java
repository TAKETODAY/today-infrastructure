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

package cn.taketoday.framework.logging.logback;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.LoggingEvent;
import cn.taketoday.framework.logging.LoggingSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/15 17:25
 */
class ApplicationNameConverterTests {

  private final ApplicationNameConverter converter;

  private final LoggingEvent event = new LoggingEvent();

  ApplicationNameConverterTests() {
    this.converter = new ApplicationNameConverter();
    this.converter.setContext(new LoggerContext());
    this.event.setLoggerContextRemoteView(
            new LoggerContextVO("test", Collections.emptyMap(), System.currentTimeMillis()));
  }

  @Test
  void whenNoLoggedApplicationNameConvertReturnsEmptyString() {
    withLoggedApplicationName(null, () -> {
      this.converter.start();
      String converted = this.converter.convert(this.event);
      assertThat(converted).isEqualTo("");
    });
  }

  @Test
  void whenLoggedApplicationNameConvertReturnsIt() {
    withLoggedApplicationName("my-application", () -> {
      this.converter.start();
      String converted = this.converter.convert(this.event);
      assertThat(converted).isEqualTo("my-application");
    });
  }

  private void withLoggedApplicationName(String name, Runnable action) {
    if (name == null) {
      System.clearProperty(LoggingSystemProperty.APPLICATION_NAME.getEnvironmentVariableName());
    }
    else {
      System.setProperty(LoggingSystemProperty.APPLICATION_NAME.getEnvironmentVariableName(), name);
    }
    try {
      action.run();
    }
    finally {
      System.clearProperty(LoggingSystemProperty.APPLICATION_NAME.getEnvironmentVariableName());
    }
  }

}