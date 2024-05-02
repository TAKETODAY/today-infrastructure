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

package cn.taketoday.app.loader.net.protocol.jar;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;

import cn.taketoday.app.loader.net.protocol.Handlers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarFileUrlKey}.
 *
 * @author Phillip Webb
 */
class JarFileUrlKeyTests {

  @BeforeAll
  static void setup() {
    Handlers.register();
  }

  @Test
  void getCreatesKey() throws Exception {
    URL url = new URL("jar:nested:/my.jar/!mynested.jar!/my/path");
    assertThat(JarFileUrlKey.get(url)).isEqualTo("jar:nested:/my.jar/!mynested.jar!/my/path");
  }

  @Test
  void getWhenUppercaseProtocolCreatesKey() throws Exception {
    URL url = new URL("JAR:nested:/my.jar/!mynested.jar!/my/path");
    assertThat(JarFileUrlKey.get(url)).isEqualTo("jar:nested:/my.jar/!mynested.jar!/my/path");
  }

  @Test
  void getWhenHasHostAndPortCreatesKey() throws Exception {
    URL url = new URL("https://example.com:1234/test");
    assertThat(JarFileUrlKey.get(url)).isEqualTo("https:example.com:1234/test");
  }

  @Test
  void getWhenHasUppercaseHostCreatesKey() throws Exception {
    URL url = new URL("https://EXAMPLE.com:1234/test");
    assertThat(JarFileUrlKey.get(url)).isEqualTo("https:example.com:1234/test");
  }

  @Test
  void getWhenHasNoPortCreatesKeyWithDefaultPort() throws Exception {
    URL url = new URL("https://EXAMPLE.com/test");
    assertThat(JarFileUrlKey.get(url)).isEqualTo("https:example.com:443/test");
  }

  @Test
  void getWhenHasNoFileCreatesKey() throws Exception {
    URL url = new URL("https://EXAMPLE.com");
    assertThat(JarFileUrlKey.get(url)).isEqualTo("https:example.com:443");
  }

  @Test
  void getWhenHasRuntimeRefCreatesKey() throws Exception {
    URL url = new URL("jar:nested:/my.jar/!mynested.jar!/my/path#runtime");
    assertThat(JarFileUrlKey.get(url)).isEqualTo("jar:nested:/my.jar/!mynested.jar!/my/path#runtime");
  }

  @Test
  void getWhenHasOtherRefCreatesKeyWithoutRef() throws Exception {
    URL url = new URL("jar:nested:/my.jar/!mynested.jar!/my/path#example");
    assertThat(JarFileUrlKey.get(url)).isEqualTo("jar:nested:/my.jar/!mynested.jar!/my/path");
  }

}
