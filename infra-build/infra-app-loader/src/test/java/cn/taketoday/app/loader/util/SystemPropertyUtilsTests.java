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

package cn.taketoday.app.loader.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemPropertyUtils}.
 *
 * @author Dave Syer
 */
class SystemPropertyUtilsTests {

  @BeforeEach
  void init() {
    System.setProperty("foo", "bar");
  }

  @AfterEach
  void close() {
    System.clearProperty("foo");
  }

  @Test
  void testVanillaPlaceholder() {
    assertThat(SystemPropertyUtils.resolvePlaceholders("${foo}")).isEqualTo("bar");
  }

  @Test
  void testDefaultValue() {
    assertThat(SystemPropertyUtils.resolvePlaceholders("${bar:foo}")).isEqualTo("foo");
  }

  @Test
  void testNestedPlaceholder() {
    assertThat(SystemPropertyUtils.resolvePlaceholders("${bar:${spam:foo}}")).isEqualTo("foo");
  }

  @Test
  void testEnvVar() {
    assertThat(SystemPropertyUtils.getProperty("lang")).isEqualTo(System.getenv("LANG"));
  }

}
