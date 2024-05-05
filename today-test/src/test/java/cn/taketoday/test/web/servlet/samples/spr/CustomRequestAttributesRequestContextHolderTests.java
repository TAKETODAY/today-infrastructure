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

package cn.taketoday.test.web.servlet.samples.spr;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.mock.web.MockServletContext;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.config.WebMvcConfigurer;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.support.GenericWebApplicationContext;
import cn.taketoday.mock.api.ServletContext;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SPR-13211 which verify that a custom mock request
 * is not reused by MockMvc.
 *
 * @author Sam Brannen
 * @see RequestContextHolderTests
 * @since 4.0
 */
class CustomRequestAttributesRequestContextHolderTests {

  private static final String FROM_CUSTOM_MOCK = "fromCustomMock";
  private static final String FROM_MVC_TEST_DEFAULT = "fromSpringMvcTestDefault";
  private static final String FROM_MVC_TEST_MOCK = "fromSpringMvcTestMock";

  private final GenericWebApplicationContext wac = new GenericWebApplicationContext();

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    ServletContext servletContext = new MockServletContext();
    MockHttpServletRequest mockRequest = new MockHttpServletRequest(servletContext);
    mockRequest.setAttribute(FROM_CUSTOM_MOCK, FROM_CUSTOM_MOCK);
    RequestContextHolder.set(new ServletRequestContext(null, mockRequest, new MockHttpServletResponse()));

    this.wac.setServletContext(servletContext);
    new AnnotatedBeanDefinitionReader(this.wac).register(WebConfig.class);
    this.wac.refresh();

    this.mockMvc = webAppContextSetup(this.wac)
            .defaultRequest(get("/").requestAttr(FROM_MVC_TEST_DEFAULT, FROM_MVC_TEST_DEFAULT))
            .alwaysExpect(status().isOk())
            .build();
  }

  @Test
  void singletonController() throws Exception {
    this.mockMvc.perform(get("/singletonController").requestAttr(FROM_MVC_TEST_MOCK, FROM_MVC_TEST_MOCK));
  }

  @AfterEach
  void verifyCustomRequestAttributesAreRestored() {
    RequestContext context = RequestContextHolder.getRequired();

    assertThat(context.getAttribute(FROM_CUSTOM_MOCK)).isEqualTo(FROM_CUSTOM_MOCK);
    assertThat(context.getAttribute(FROM_MVC_TEST_DEFAULT)).isNull();
    assertThat(context.getAttribute(FROM_MVC_TEST_MOCK)).isNull();

    RequestContextHolder.cleanup();
    this.wac.close();
  }

  // -------------------------------------------------------------------

  @Configuration
  @EnableWebMvc
  static class WebConfig implements WebMvcConfigurer {

    @Bean
    public SingletonController singletonController() {
      return new SingletonController();
    }
  }

  @RestController
  private static class SingletonController {

    @RequestMapping("/singletonController")
    public void handle() {
      assertRequestAttributes();
    }
  }

  private static void assertRequestAttributes() {
    RequestContext request = RequestContextHolder.getRequired();
    assertRequestAttributes(request);
  }

  private static void assertRequestAttributes(RequestContext request) {
    assertThat(request.getAttribute(FROM_CUSTOM_MOCK)).isNull();
    assertThat(request.getAttribute(FROM_MVC_TEST_DEFAULT)).isEqualTo(FROM_MVC_TEST_DEFAULT);
    assertThat(request.getAttribute(FROM_MVC_TEST_MOCK)).isEqualTo(FROM_MVC_TEST_MOCK);
  }

}
