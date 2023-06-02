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

import org.assertj.core.util.Throwables;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import cn.taketoday.annotation.config.web.WebMvcProperties;
import cn.taketoday.context.properties.bind.BindException;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link WebMvcProperties}.
 *
 * @author Stephane Nicoll
 */
class WebMvcPropertiesTests {

  private final WebMvcProperties properties = new WebMvcProperties();

  @Test
  void servletPathWhenEndsWithSlashHasValidMappingAndPrefix() {
    bind("web.mvc.servlet.path", "/foo/");
    assertThat(this.properties.getServlet().getServletMapping()).isEqualTo("/foo/*");
    assertThat(this.properties.getServlet().getServletPrefix()).isEqualTo("/foo");
  }

  @Test
  void servletPathWhenDoesNotEndWithSlashHasValidMappingAndPrefix() {
    bind("web.mvc.servlet.path", "/foo");
    assertThat(this.properties.getServlet().getServletMapping()).isEqualTo("/foo/*");
    assertThat(this.properties.getServlet().getServletPrefix()).isEqualTo("/foo");
  }

  @Test
  void servletPathWhenHasWildcardThrowsException() {
    assertThatExceptionOfType(BindException.class).isThrownBy(() -> bind("web.mvc.servlet.path", "/*"))
            .withRootCauseInstanceOf(IllegalArgumentException.class).satisfies(
                    (ex) -> assertThat(Throwables.getRootCause(ex)).hasMessage("Path must not contain wildcards"));
  }

  private void bind(String name, String value) {
    bind(Collections.singletonMap(name, value));
  }

  private void bind(Map<String, String> map) {
    ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
    new Binder(source).bind("web.mvc", Bindable.ofInstance(this.properties));
  }

}
