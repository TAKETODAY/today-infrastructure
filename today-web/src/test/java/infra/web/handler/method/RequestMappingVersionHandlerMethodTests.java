/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.web.annotation.GetMapping;
import infra.web.annotation.RestController;
import infra.web.config.EnableWebMvc;
import infra.web.config.WebMvcConfigurer;
import infra.web.config.annotation.ApiVersionConfigurer;
import infra.web.mock.support.AnnotationConfigWebApplicationContext;
import jakarta.servlet.ServletException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for API versioning.
 *
 * @author Rossen Stoyanchev
 */
public class RequestMappingVersionHandlerMethodTests {

  private DispatcherServlet dispatcherServlet;

  @BeforeEach
  void setUp() throws ServletException {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.setServletConfig(new MockServletConfig());
    context.register(WebConfig.class, TestController.class);
    context.afterPropertiesSet();

    this.dispatcherServlet = new DispatcherServlet(context);
    this.dispatcherServlet.init(new MockServletConfig());
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

    MockHttpServletResponse response = requestWithVersion("1.6");
    assertThat(response.getStatus()).isEqualTo(400);
  }

  private MockHttpServletResponse requestWithVersion(String version) throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
    request.addHeader("X-API-VERSION", version);
    MockHttpServletResponse response = new MockHttpServletResponse();
    this.dispatcherServlet.service(request, response);
    return response;
  }

  @EnableWebMvc
  private static class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
      configurer.useRequestHeader("X-API-Version").addSupportedVersions("1", "1.1", "1.3", "1.6");
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

    @GetMapping(version = "1.5")
    String version1_5() {
      return getBody("1.5");
    }

    private static String getBody(String version) {
      return version;
    }
  }

}
