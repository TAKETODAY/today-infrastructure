/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.server.reactive;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.web.testfixture.http.server.reactive.MockServerHttpRequest;
import cn.taketoday.web.testfixture.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ContextPathCompositeHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class ContextPathCompositeHandlerTests {

  @Test
  public void invalidContextPath() {
    testInvalid("  ", "Context path must not be empty");
    testInvalid("path", "Context path must begin with '/'");
    testInvalid("/path/", "Context path must not end with '/'");
  }

  private void testInvalid(String contextPath, String expectedError) {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    new ContextPathCompositeHandler(Collections.singletonMap(contextPath, new TestHttpHandler())))
            .withMessage(expectedError);
  }

  @Test
  public void match() {
    TestHttpHandler handler1 = new TestHttpHandler();
    TestHttpHandler handler2 = new TestHttpHandler();
    TestHttpHandler handler3 = new TestHttpHandler();

    Map<String, HttpHandler> map = new HashMap<>();
    map.put("/path", handler1);
    map.put("/another/path", handler2);
    map.put("/yet/another/path", handler3);

    testHandle("/another/path/and/more", map);

    assertInvoked(handler2, "/another/path");
    assertNotInvoked(handler1, handler3);
  }

  @Test
  public void matchWithContextPathEqualToPath() {
    TestHttpHandler handler1 = new TestHttpHandler();
    TestHttpHandler handler2 = new TestHttpHandler();
    TestHttpHandler handler3 = new TestHttpHandler();

    Map<String, HttpHandler> map = new HashMap<>();
    map.put("/path", handler1);
    map.put("/another/path", handler2);
    map.put("/yet/another/path", handler3);

    testHandle("/path", map);

    assertInvoked(handler1, "/path");
    assertNotInvoked(handler2, handler3);
  }

  @Test
  public void matchWithNativeContextPath() {
    MockServerHttpRequest request = MockServerHttpRequest
            .get("/yet/another/path")
            .contextPath("/yet")  // contextPath in underlying request
            .build();

    TestHttpHandler handler = new TestHttpHandler();
    Map<String, HttpHandler> map = Collections.singletonMap("/another/path", handler);

    new ContextPathCompositeHandler(map).handle(request, new MockServerHttpResponse());

    assertThat(handler.wasInvoked()).isTrue();
    assertThat(handler.getRequest().getPath().contextPath().value()).isEqualTo("/yet/another/path");
  }

  @Test
  public void notFound() {
    TestHttpHandler handler1 = new TestHttpHandler();
    TestHttpHandler handler2 = new TestHttpHandler();

    Map<String, HttpHandler> map = new HashMap<>();
    map.put("/path", handler1);
    map.put("/another/path", handler2);

    ServerHttpResponse response = testHandle("/yet/another/path", map);

    assertNotInvoked(handler1, handler2);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test // SPR-17144
  public void notFoundWithCommitAction() {

    AtomicBoolean commitInvoked = new AtomicBoolean();

    ServerHttpRequest request = MockServerHttpRequest.get("/unknown/path").build();
    ServerHttpResponse response = new MockServerHttpResponse();
    response.beforeCommit(() -> {
      commitInvoked.set(true);
      return Mono.empty();
    });

    Map<String, HttpHandler> map = new HashMap<>();
    TestHttpHandler handler = new TestHttpHandler();
    map.put("/path", handler);
    new ContextPathCompositeHandler(map).handle(request, response).block(Duration.ofSeconds(5));

    assertNotInvoked(handler);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(commitInvoked.get()).isTrue();
  }

  private ServerHttpResponse testHandle(String pathToHandle, Map<String, HttpHandler> handlerMap) {
    ServerHttpRequest request = MockServerHttpRequest.get(pathToHandle).build();
    ServerHttpResponse response = new MockServerHttpResponse();
    new ContextPathCompositeHandler(handlerMap).handle(request, response).block(Duration.ofSeconds(5));
    return response;
  }

  private void assertInvoked(TestHttpHandler handler, String contextPath) {
    assertThat(handler.wasInvoked()).isTrue();
    assertThat(handler.getRequest().getPath().contextPath().value()).isEqualTo(contextPath);
  }

  private void assertNotInvoked(TestHttpHandler... handlers) {
    Arrays.stream(handlers).forEach(handler -> assertThat(handler.wasInvoked()).isFalse());
  }

  @SuppressWarnings("WeakerAccess")
  private static class TestHttpHandler implements HttpHandler {

    private ServerHttpRequest request;

    public boolean wasInvoked() {
      return (this.request != null);
    }

    public ServerHttpRequest getRequest() {
      return this.request;
    }

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      this.request = request;
      return Mono.empty();
    }
  }

}
