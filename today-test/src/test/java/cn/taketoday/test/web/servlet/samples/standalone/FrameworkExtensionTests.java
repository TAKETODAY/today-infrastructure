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

package cn.taketoday.test.web.servlet.samples.standalone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.test.web.servlet.request.RequestPostProcessor;
import cn.taketoday.test.web.servlet.setup.MockMvcConfigurer;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseBody;

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

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockMvc = standaloneSetup(new SampleController()).apply(defaultSetup()).build();
  }

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
   * Test {@code RequestPostProcessor}.
   */
  private static class TestRequestPostProcessor implements RequestPostProcessor {

    private final HttpHeaders headers = HttpHeaders.create();

    public TestRequestPostProcessor foo(String value) {
      this.headers.add("Foo", value);
      return this;
    }

    public TestRequestPostProcessor bar(String value) {
      this.headers.add("Bar", value);
      return this;
    }

    @Override
    public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
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
    public RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder,
            WebApplicationContext context) {
      return request -> {
        request.setUserPrincipal(mock(Principal.class));
        return request;
      };
    }
  }

  @Controller
  @RequestMapping("/")
  private static class SampleController {

    @RequestMapping(headers = "Foo")
    @ResponseBody
    public String handleFoo(Principal principal) {
      Assert.notNull(principal, "Principal must not be null");
      return "Foo";
    }

    @RequestMapping(headers = "Bar")
    @ResponseBody
    public String handleBar(Principal principal) {
      Assert.notNull(principal, "Principal must not be null");
      return "Bar";
    }
  }

}
