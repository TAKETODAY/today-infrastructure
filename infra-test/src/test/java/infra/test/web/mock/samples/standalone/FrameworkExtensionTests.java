/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.web.mock.samples.standalone;

import org.junit.jupiter.api.Test;

import infra.context.ApplicationContext;
import infra.http.HttpHeaders;
import infra.mock.web.HttpMockRequestImpl;
import infra.stereotype.Controller;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.request.RequestPostProcessor;
import infra.test.web.mock.setup.ConfigurableMockMvcBuilder;
import infra.test.web.mock.setup.MockMvcConfigurer;
import infra.test.web.mock.setup.MockMvcConfigurerAdapter;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseBody;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
import static org.mockito.Mockito.mock;

/**
 * Demonstrates use of SPI extension points:
 * <ul>
 * <li> {@link RequestPostProcessor}
 * for extending request building with custom methods.
 * <li> {@link MockMvcConfigurer
 * MockMvcConfigurer} for extending MockMvc building with some automatic setup.
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class FrameworkExtensionTests {

  private final MockMvc mockMvc = standaloneSetup(new SampleController()).apply(defaultSetup()).build();

  @Test
  public void fooHeader() throws Exception {
    this.mockMvc.perform(get("/").with(headers().foo("a=b"))).andExpect(content().string("Foo"));
  }

  @Test
  public void barHeader() throws Exception {
    this.mockMvc.perform(get("/").with(headers().bar("a=b"))).andExpect(content().string("Bar"));
  }

  private static TestMockMvcConfigurer defaultSetup() {
    return new TestMockMvcConfigurer();
  }

  private static TestRequestPostProcessor headers() {
    return new TestRequestPostProcessor();
  }

  /**
   * Test {@code RequestPostProcessor} for custom headers.
   */
  private static class TestRequestPostProcessor implements RequestPostProcessor {

    private final HttpHeaders headers = HttpHeaders.forWritable();

    public TestRequestPostProcessor foo(String value) {
      this.headers.add("Foo", value);
      return this;
    }

    public TestRequestPostProcessor bar(String value) {
      this.headers.add("Bar", value);
      return this;
    }

    @Override
    public HttpMockRequestImpl postProcessRequest(HttpMockRequestImpl request) {
      for (String headerName : this.headers.keySet()) {
        request.addHeader(headerName, this.headers.get(headerName));
      }
      return request;
    }
  }

  /**
   * Test {@code MockMvcConfigurer}.
   */
  private static class TestMockMvcConfigurer extends MockMvcConfigurerAdapter {

    @Override
    public void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
      builder.alwaysExpect(status().isOk());
    }

    @Override
    public RequestPostProcessor beforeMockMvcCreated(
            ConfigurableMockMvcBuilder<?> builder, ApplicationContext context) {

      return request -> {
        request.setUserPrincipal(mock());
        return request;
      };
    }
  }

  @Controller
  @RequestMapping("/")
  private static class SampleController {

    @RequestMapping(headers = "Foo")
    @ResponseBody
    public String handleFoo() {
      return "Foo";
    }

    @RequestMapping(headers = "Bar")
    @ResponseBody
    public String handleBar() {
      return "Bar";
    }
  }

}
