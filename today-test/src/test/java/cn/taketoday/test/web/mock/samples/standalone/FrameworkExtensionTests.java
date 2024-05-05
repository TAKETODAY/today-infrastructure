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

package cn.taketoday.test.web.mock.samples.standalone;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.mock.MockMvc;
import cn.taketoday.test.web.mock.request.RequestPostProcessor;
import cn.taketoday.test.web.mock.setup.ConfigurableMockMvcBuilder;
import cn.taketoday.test.web.mock.setup.MockMvcConfigurerAdapter;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseBody;

import static cn.taketoday.test.web.mock.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
import static org.mockito.Mockito.mock;

/**
 * Demonstrates use of SPI extension points:
 * <ul>
 * <li> {@link cn.taketoday.test.web.mock.request.RequestPostProcessor}
 * for extending request building with custom methods.
 * <li> {@link cn.taketoday.test.web.mock.setup.MockMvcConfigurer
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
