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

package infra.test.web.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.ComponentScan;
import infra.context.annotation.Configuration;
import infra.http.HttpMethod;
import infra.stereotype.Controller;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.web.WebAppConfiguration;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.web.RequestContext;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseBody;
import infra.web.client.HttpClientErrorException;
import infra.web.client.RestTemplate;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.mock.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests that use a {@link RestTemplate} configured with a
 * {@link MockMvcClientHttpRequestFactory} that is in turn configured with a
 * {@link MockMvc} instance that uses a {@link WebApplicationContext} loaded by
 * the TestContext framework.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/2 21:29
 */
@ExtendWith(InfraExtension.class)
@WebAppConfiguration
@ContextConfiguration
public class MockMvcClientHttpRequestFactoryTests {

  @Autowired
  private WebApplicationContext wac;

  private RestTemplate template;

  @BeforeEach
  public void setup() {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    this.template = new RestTemplate(new MockMvcClientHttpRequestFactory(mockMvc));
  }

  @Test
  public void withResult() {
    assertThat(template.getForObject("/foo", String.class)).isEqualTo("bar");
  }

  @Test
  public void withError() {
    assertThatExceptionOfType(HttpClientErrorException.class)
            .isThrownBy(() -> template.getForEntity("/error", String.class))
            .withMessageContaining("400")
            .withMessageContaining("some bad request");
  }

  @Test
  public void withErrorAndBody() {
    assertThatExceptionOfType(HttpClientErrorException.class)
            .isThrownBy(() -> template.getForEntity("/errorbody", String.class))
            .withMessageContaining("400")
            .withMessageContaining("some really bad request");
  }

  @EnableWebMvc
  @Configuration
  @ComponentScan(basePackageClasses = MockMvcClientHttpRequestFactoryTests.class)
  static class MyWebConfig implements WebMvcConfigurer {
  }

  @Controller
  static class MyController {

    @RequestMapping(value = "/foo", method = HttpMethod.GET)
    @ResponseBody
    public String handle() {
      return "bar";
    }

    @RequestMapping(value = "/error", method = HttpMethod.GET)
    public void handleError(RequestContext response) throws Exception {
      response.sendError(400, "some bad request");
    }

    @RequestMapping(value = "/errorbody", method = HttpMethod.GET)
    public void handleErrorWithBody(RequestContext response) throws Exception {
      response.sendError(400, "some bad request");
      response.getWriter().write("some really bad request");
    }
  }

}
