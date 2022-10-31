/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.taketoday.annotation.config.web.servlet;

import org.assertj.core.util.Throwables;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import cn.taketoday.context.properties.bind.BindException;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.MapConfigurationPropertySource;
import cn.taketoday.web.config.WebMvcProperties;

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
