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

package cn.taketoday.framework.web.embedded.tomcat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TldPatterns}.
 *
 * @author Phillip Webb
 */
class TldPatternsTests {

  @Test
  void tomcatSkipAlignsWithTomcatDefaults() throws IOException {
    assertThat(TldPatterns.TOMCAT_SKIP).containsExactlyInAnyOrderElementsOf(getTomcatDefaultJarsToSkip());
  }

  @Test
  void tomcatScanAlignsWithTomcatDefaults() throws IOException {
    assertThat(TldPatterns.TOMCAT_SCAN).containsExactlyInAnyOrderElementsOf(getTomcatDefaultJarsToScan());
  }

  private Set<String> getTomcatDefaultJarsToSkip() throws IOException {
    return getTomcatDefault("tomcat.util.scan.StandardJarScanFilter.jarsToSkip");
  }

  private Set<String> getTomcatDefaultJarsToScan() throws IOException {
    return getTomcatDefault("tomcat.util.scan.StandardJarScanFilter.jarsToScan");
  }

  private Set<String> getTomcatDefault(String key) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    try (InputStream inputStream = classLoader.getResource("catalina.properties").openStream()) {
      Properties properties = new Properties();
      properties.load(inputStream);
      String jarsToSkip = properties.getProperty(key);
      return StringUtils.commaDelimitedListToSet(jarsToSkip);
    }
  }

}
