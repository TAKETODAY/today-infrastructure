/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.app.test.mock.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import infra.app.test.context.InfraApplicationContextLoader;
import infra.context.annotation.Configuration;
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
