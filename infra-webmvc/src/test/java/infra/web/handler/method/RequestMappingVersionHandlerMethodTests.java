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

package infra.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import infra.mock.api.MockException;
import infra.mock.api.http.HttpMockResponse;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.mock.web.MockMockConfig;
import infra.web.accept.StandardApiVersionDeprecationHandler;
import infra.web.annotation.GET;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RestController;
import infra.web.config.annotation.ApiVersionConfigurer;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.mock.MockDispatcher;
import infra.web.mock.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for API versioning.
 *
 * @author Rossen Stoyanchev
 */
public class RequestMappingVersionHandlerMethodTests {

  private MockDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.setMockConfig(new MockMockConfig());
    context.register(WebConfig.class, TestController.class);
    context.afterPropertiesSet();

    this.dispatcher = new MockDispatcher(context);
    this.dispatcher.init(new MockMockConfig());
  }

  @Test
  void initialVersion() throws Exception {
    assertThat(requestWithVersion("1.0").getContentAsString()).isEqualTo("none");
    assertThat(requestWithVersion("1.1").getContentAsString()).isEqualTo("none");
  }

  @Test
  void baselineVersion() throws Exception {
    assertThat(requestWithVersion("1.2").getContentAsString()).isEqualTo("1.2");
    assertThat(requestWithVersion("1.3").getContentAsString()).isEqualTo("1.2");
  }

  @Test
  void fixedVersion() throws Exception {
    assertThat(requestWithVersion("1.5").getContentAsString()).isEqualTo("1.5");

    HttpMockResponse response = requestWithVersion("1.6");
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  void deprecation() throws Exception {
    assertThat(requestWithVersion("1").getHeader("Link"))
            .isEqualTo("<https://example.org/deprecation>; rel=\"deprecation\"; type=\"text/html\"");
  }

  private MockHttpResponseImpl requestWithVersion(String version) throws MockException {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.addHeader("X-API-VERSION", version);
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    this.dispatcher.service(request, response);
    return response;
  }

  @EnableWebMvc
  private static class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {

      StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler();
      handler.configureVersion("1").setDeprecationLink(URI.create("https://example.org/deprecation"));

      configurer.useRequestHeader("X-API-Version")
              .addSupportedVersions("1", "1.1", "1.3", "1.6")
              .setDeprecationHandler(handler);
    }
  }

  @RestController
  private static class TestController {

    @GetMapping
    String noVersion() {
      return getBody("none");
    }

    @GetMapping(version = "1.2+")
    String version1_2() {
      return getBody("1.2");
    }

    @GET(version = "1.5")
    String version1_5() {
      return getBody("1.5");
    }

    private static String getBody(String version) {
      return version;
    }
  }

}
