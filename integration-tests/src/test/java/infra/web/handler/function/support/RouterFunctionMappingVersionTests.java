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

package infra.web.handler.function.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import infra.context.annotation.Bean;
import infra.mock.api.MockContext;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.accept.StandardApiVersionDeprecationHandler;
import infra.web.config.annotation.ApiVersionConfigurer;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.handler.HandlerExecutionChain;
import infra.web.handler.function.HandlerFunction;
import infra.web.handler.function.RouterFunction;
import infra.web.handler.function.RouterFunctions;
import infra.web.handler.function.ServerRequest;
import infra.web.handler.function.ServerResponse;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.AnnotationConfigWebApplicationContext;

import static infra.web.handler.function.RequestPredicates.version;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RouterFunctionMapping} integration tests for API versioning.
 *
 * @author Rossen Stoyanchev
 */
public class RouterFunctionMappingVersionTests {

  private final MockContext mockContext = new MockContextImpl();

  private RouterFunctionMapping mapping;

  @BeforeEach
  void setUp() {
    AnnotationConfigWebApplicationContext wac = new AnnotationConfigWebApplicationContext();
    wac.setMockContext(this.mockContext);
    wac.register(WebConfig.class);
    wac.refresh();

    this.mapping = wac.getBean(RouterFunctionMapping.class);
  }

  @Test
  void mapVersion() throws Exception {
    testGetHandler("1.0", "none");
    testGetHandler("1.1", "none");
    testGetHandler("1.2", "1.2");
    testGetHandler("1.3", "1.2");
    testGetHandler("1.5", "1.5");
  }

  @Test
  void deprecation() throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.addHeader("X-API-Version", "1");

    MockHttpResponseImpl response = new MockHttpResponseImpl();

    MockRequestContext context = new MockRequestContext(request, response);
    HandlerExecutionChain chain = (HandlerExecutionChain) this.mapping.getHandler(context);
    assertThat(chain).isNotNull();

    context.requestCompleted();

    assertThat(((TestHandler) chain.getRawHandler()).body()).isEqualTo("none");
    assertThat(response.getHeader("Link"))
            .isEqualTo("<https://example.org/deprecation>; rel=\"deprecation\"; type=\"text/html\"");
  }

  private void testGetHandler(String version, String expectedBody) throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");
    request.addHeader("X-API-Version", version);
    HandlerExecutionChain chain = (HandlerExecutionChain) this.mapping.getHandler(new MockRequestContext(request));
    HandlerFunction<?> handler = (HandlerFunction<?>) chain.getRawHandler();
    assertThat(((TestHandler) handler).body()).isEqualTo(expectedBody);
  }

  @EnableWebMvc
  private static class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
      StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler();
      handler.configureVersion("1").setDeprecationLink(URI.create("https://example.org/deprecation"));

      configurer.useRequestHeader("X-API-Version")
              .addSupportedVersions("1", "1.1", "1.3")
              .setDeprecationHandler(handler);
    }

    @Bean
    RouterFunction<?> routerFunction() {
      return RouterFunctions.route()
              .path("/", builder -> builder
                      .GET(version("1.5"), new TestHandler("1.5"))
                      .GET(version("1.2+"), new TestHandler("1.2"))
                      .GET(new TestHandler("none")))
              .build();
    }
  }

  private record TestHandler(String body) implements HandlerFunction<ServerResponse> {

    @Override
    public ServerResponse handle(ServerRequest request) {
      return ServerResponse.ok().body(body);
    }
  }

}
