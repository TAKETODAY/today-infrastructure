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

package cn.taketoday.annotation.config.web.servlet;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/2 21:53
 */
class DispatcherServletPathTests {

  @Test
  void getRelativePathReturnsRelativePath() {
    assertThat(((DispatcherServletPath) () -> "spring").getRelativePath("boot")).isEqualTo("spring/boot");
    assertThat(((DispatcherServletPath) () -> "spring/").getRelativePath("boot")).isEqualTo("spring/boot");
    assertThat(((DispatcherServletPath) () -> "spring").getRelativePath("/boot")).isEqualTo("spring/boot");
  }

  @Test
  void getPrefixWhenHasSimplePathReturnPath() {
    assertThat(((DispatcherServletPath) () -> "spring").getPrefix()).isEqualTo("spring");
  }

  @Test
  void getPrefixWhenHasPatternRemovesPattern() {
    assertThat(((DispatcherServletPath) () -> "spring/*.do").getPrefix()).isEqualTo("spring");
  }

  @Test
  void getPathWhenPathEndsWithSlashRemovesSlash() {
    assertThat(((DispatcherServletPath) () -> "spring/").getPrefix()).isEqualTo("spring");
  }

  @Test
  void getServletUrlMappingWhenPathIsEmptyReturnsSlash() {
    assertThat(((DispatcherServletPath) () -> "").getServletUrlMapping()).isEqualTo("/");
  }

  @Test
  void getServletUrlMappingWhenPathIsSlashReturnsSlash() {
    assertThat(((DispatcherServletPath) () -> "/").getServletUrlMapping()).isEqualTo("/");
  }

  @Test
  void getServletUrlMappingWhenPathContainsStarReturnsPath() {
    assertThat(((DispatcherServletPath) () -> "spring/*.do").getServletUrlMapping()).isEqualTo("spring/*.do");
  }

  @Test
  void getServletUrlMappingWhenHasPathNotEndingSlashReturnsSlashStarPattern() {
    assertThat(((DispatcherServletPath) () -> "spring/boot").getServletUrlMapping()).isEqualTo("spring/boot/*");
  }

  @Test
  void getServletUrlMappingWhenHasPathEndingWithSlashReturnsSlashStarPattern() {
    assertThat(((DispatcherServletPath) () -> "spring/boot/").getServletUrlMapping()).isEqualTo("spring/boot/*");
  }

}