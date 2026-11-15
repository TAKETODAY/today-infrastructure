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

package infra.web.handler.function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.http.MediaType;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.mock.MockDispatcherHandler;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for functional web endpoints handled through the
 * {@code ReturnValueHandler} chain.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class RouterFunctionIntegrationTests {

  private AnnotationConfigApplicationContext context;

  private MockDispatcherHandler dispatcherHandler;

  @BeforeEach
  void setUp() {
    this.context = new AnnotationConfigApplicationContext();
    this.context.register(WebConfig.class);
    this.context.refresh();
    this.dispatcherHandler = new MockDispatcherHandler(this.context);
    this.dispatcherHandler.start();
  }

  @AfterEach
  void tearDown() {
    this.context.close();
  }

  @Test
  void serverResponseBody() throws Exception {
    MockResponse response = get("/hello");

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("hello");
  }

  @Test
  void serverResponseStatus() throws Exception {
    MockResponse response = get("/created");

    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getContentAsString()).isEmpty();
  }

  @Test
  void serverResponseJson() throws Exception {
    MockResponse response = get("/json");

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEqualTo("{\"name\":\"John\",\"age\":42}");
  }

  @Test
  void serverResponseSse() throws Exception {
    MockResponse response = get("/sse");

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentType()).isEqualTo(MediaType.TEXT_EVENT_STREAM_VALUE);
    assertThat(response.getContentAsString()).isEqualTo("event:greeting\ndata: hello\n\n");
  }

  private MockResponse get(String path) throws Exception {
    MockRequest request = new MockRequest("GET", path);
    request.setAsyncSupported(true);
    MockResponse response = new MockResponse();
    this.dispatcherHandler.service(request, response);
    return response;
  }

  @Configuration
  @EnableWebMvc
  static class WebConfig {

    @Bean
    RouterFunction<ServerResponse> routerFunction() {
      return RouterFunctions.route()
              .GET("/hello", request -> ServerResponse.ok().body("hello"))
              .GET("/created", request -> ServerResponse.status(201).build())
              .GET("/json", request -> ServerResponse.ok().body(new Person("John", 42)))
              .GET("/sse", request -> ServerResponse.sse(sse -> {
                try {
                  sse.event("greeting").data("hello");
                }
                catch (java.io.IOException ex) {
                  throw new java.io.UncheckedIOException(ex);
                }
                sse.complete();
              }))
              .build();
    }
  }

  public record Person(String name, int age) {
  }

}
