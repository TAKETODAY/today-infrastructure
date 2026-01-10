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

package infra.test.web.mock.samples.spr;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.test.web.mock.MockMvc;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.GenericWebApplicationContext;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;

/**
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
    MockContextImpl mockContext = new MockContextImpl();
    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl(mockContext);
    mockRequest.setAttribute(FROM_CUSTOM_MOCK, FROM_CUSTOM_MOCK);
    RequestContextHolder.set(new MockRequestContext(null, mockRequest, new MockHttpResponseImpl()));

    this.wac.setMockContext(mockContext);
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
