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

package infra.app.test.mock.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import infra.context.annotation.Configuration;
import infra.app.test.context.InfraApplicationContextLoader;
import infra.mock.api.MockContext;
import infra.test.annotation.DirtiesContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.web.WebAppConfiguration;
import infra.web.mock.MockContextAware;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/11 17:27
 */
@DirtiesContext
@ExtendWith(InfraExtension.class)
@ContextConfiguration(loader = InfraApplicationContextLoader.class)
@WebAppConfiguration("src/test/webapp")
class InfraMockContextTests implements MockContextAware {

  private MockContext mockContext;

  @Override
  public void setMockContext(MockContext mockContext) {
    this.mockContext = mockContext;
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
    URL resource = this.mockContext.getResource(path);
    assertThat(resource).isNotNull();
    assertThat(resource.getPath()).contains(expectedLocation);
  }

  // gh-2654
  @Test
  void getRootUrlExistsAndIsEmpty() throws Exception {
    InfraMockContext context = new InfraMockContext("src/test/doesntexist") {
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
