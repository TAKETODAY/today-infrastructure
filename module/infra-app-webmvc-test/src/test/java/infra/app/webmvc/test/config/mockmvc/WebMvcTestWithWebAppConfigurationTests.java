/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.webmvc.test.config.mockmvc;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import infra.app.webmvc.test.config.WebMvcTest;
import infra.beans.factory.annotation.Autowired;
import infra.mock.api.MockContext;
import infra.test.context.web.WebAppConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} when loading resources through the
 * {@link MockContext} with {@link WebAppConfiguration @WebAppConfiguration}.
 *
 * @author Lorenzo Dee
 */
@WebMvcTest
@WebAppConfiguration("src/test/webapp")
class WebMvcTestWithWebAppConfigurationTests {

  @Autowired
  private MockContext mockContext;

  @Test
  void whenBasePathIsCustomizedResourcesCanBeLoadedFromThatLocation() throws Exception {
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

}
