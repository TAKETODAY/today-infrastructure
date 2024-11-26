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

package infra.app.logging.logback;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import infra.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultLogbackConfiguration}
 *
 * @author Phillip Webb
 */
class DefaultLogbackConfigurationTests {

  @Test
  void defaultLogbackXmlContainsConsoleLogPattern() throws Exception {
    assertThatDefaultXmlContains("CONSOLE_LOG_PATTERN", DefaultLogbackConfiguration.CONSOLE_LOG_PATTERN);
  }

  @Test
  void defaultLogbackXmlContainsFileLogPattern() throws Exception {
    assertThatDefaultXmlContains("FILE_LOG_PATTERN", DefaultLogbackConfiguration.FILE_LOG_PATTERN);
  }

  private void assertThatDefaultXmlContains(String name, String value) throws Exception {
    String expected = "<property name=\"%s\" value=\"%s\"/>".formatted(name, value);
    assertThat(defaultXmlContent()).contains(expected);
  }

  private String defaultXmlContent() throws IOException {
    return StreamUtils.copyToString(getClass().getResourceAsStream("defaults.xml"), StandardCharsets.UTF_8);
  }

}
