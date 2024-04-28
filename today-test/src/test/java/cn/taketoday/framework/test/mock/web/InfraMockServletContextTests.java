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

package cn.taketoday.framework.test.mock.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.test.context.InfraApplicationContextLoader;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.web.servlet.ServletContextAware;
import cn.taketoday.web.mock.ServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/11 17:27
 */
@DirtiesContext
@ExtendWith(InfraExtension.class)
@ContextConfiguration(loader = InfraApplicationContextLoader.class)
@WebAppConfiguration("src/test/webapp")
class InfraMockServletContextTests implements ServletContextAware {

  private ServletContext servletContext;

  @Override
  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  @Test
  void getResourceLocation() throws Exception {
    testResource("/inwebapp", "src/test/webapp");
    testResource("/inmetainfresources", "/META-INF/resources");
    testResource("/inresources", "/resources");
    testResource("/instatic", "/static");
    testResource("/inpublic", "/public");
  }

  private void testResource(String path, String expectedLocation) throws MalformedURLException {
    URL resource = this.servletContext.getResource(path);
    assertThat(resource).isNotNull();
    assertThat(resource.getPath()).contains(expectedLocation);
  }

  // gh-2654
  @Test
  void getRootUrlExistsAndIsEmpty() throws Exception {
    InfraMockServletContext context = new InfraMockServletContext("src/test/doesntexist") {
      @Override
      protected String getResourceLocation(String path) {
        // Don't include the Infra Boot defaults for this test
        return getResourceBasePathLocation(path);
      }
    };
    URL resource = context.getResource("/");
    assertThat(resource).isNotNull();
    File file = new File(URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8));
    assertThat(file).exists().isDirectory();
    String[] contents = file.list((dir, name) -> !(".".equals(name) || "..".equals(name)));
    assertThat(contents).isNotNull();
    assertThat(contents).isEmpty();
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

  }

}
