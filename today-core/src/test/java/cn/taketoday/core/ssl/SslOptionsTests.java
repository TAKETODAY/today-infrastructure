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

package cn.taketoday.core.ssl;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslOptions}.
 *
 * @author Phillip Webb
 */
class SslOptionsTests {

  @Test
  void noneReturnsNull() {
    SslOptions options = SslOptions.NONE;
    assertThat(options.getCiphers()).isNull();
    assertThat(options.getEnabledProtocols()).isNull();
  }

  @Test
  void ofWithArrayCreatesSslOptions() {
    String[] ciphers = { "a", "b", "c" };
    String[] enabledProtocols = { "d", "e", "f" };
    SslOptions options = SslOptions.of(ciphers, enabledProtocols);
    assertThat(options.getCiphers()).containsExactly(ciphers);
    assertThat(options.getEnabledProtocols()).containsExactly(enabledProtocols);
  }

  @Test
  void ofWithNullArraysCreatesSslOptions() {
    String[] ciphers = null;
    String[] enabledProtocols = null;
    SslOptions options = SslOptions.of(ciphers, enabledProtocols);
    assertThat(options.getCiphers()).isNull();
    assertThat(options.getEnabledProtocols()).isNull();
  }

  @Test
  void ofWithSetCreatesSslOptions() {
    Set<String> ciphers = Set.of("a", "b", "c");
    Set<String> enabledProtocols = Set.of("d", "e", "f");
    SslOptions options = SslOptions.of(ciphers, enabledProtocols);
    assertThat(options.getCiphers()).contains("a", "b", "c");
    assertThat(options.getEnabledProtocols()).contains("d", "e", "f");
  }

  @Test
  void ofWithNullSetCreatesSslOptions() {
    Set<String> ciphers = null;
    Set<String> enabledProtocols = null;
    SslOptions options = SslOptions.of(ciphers, enabledProtocols);
    assertThat(options.getCiphers()).isNull();
    assertThat(options.getEnabledProtocols()).isNull();
  }

}
