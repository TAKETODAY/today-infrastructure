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

import java.util.Collections;

import cn.taketoday.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/2 21:52
 */
class DispatcherServletRegistrationBeanTests {

  @Test
  void createWhenPathIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new DispatcherServletRegistrationBean(new DispatcherServlet(), null))
            .withMessageContaining("Path must not be null");
  }

  @Test
  void getPathReturnsPath() {
    var bean = new DispatcherServletRegistrationBean(
            new DispatcherServlet(), "/test");
    assertThat(bean.getPath()).isEqualTo("/test");
  }

  @Test
  void getUrlMappingsReturnsSinglePathMappedPattern() {
    var bean = new DispatcherServletRegistrationBean(new DispatcherServlet(), "/test");
    assertThat(bean.getUrlMappings()).containsOnly("/test/*");
  }

  @Test
  void setUrlMappingsCannotBeCalled() {
    var bean = new DispatcherServletRegistrationBean(new DispatcherServlet(), "/test");
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> bean.setUrlMappings(Collections.emptyList()));
  }

  @Test
  void addUrlMappingsCannotBeCalled() {
    var bean = new DispatcherServletRegistrationBean(new DispatcherServlet(), "/test");
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> bean.addUrlMappings("/test"));
  }

}