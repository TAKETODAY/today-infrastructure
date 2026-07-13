/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.stereotype.Component;
import infra.util.MultiValueMapAdapter;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link DispatcherHandler#forward(AbstractHttpContext, String)} cycle detection.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class DispatcherHandlerForwardTests {

  @Configuration
  static class BaseConfig {

    @Component
    ReturnValueHandlerManager returnValueHandlerManager() {
      return new ReturnValueHandlerManager();
    }

  }

  // -- direct forward() unit tests (package-private access) --

  @Test
  void forwardToNewPathSucceeds() throws Exception {
    var handler = createHandlerWithEchoMapping();

    MockHttpContext request = new MockHttpContext();
    request.requestURI = "/source";

    handler.forward(request, "/target");

    assertThat(request.requestURI).isEqualTo("/target");
    assertThat(request.getAttribute(HttpContext.FORWARD_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
    assertThat(request.getAttribute(HttpContext.FORWARD_REQUEST_URI_ATTRIBUTE)).isEqualTo("/source");
  }

  @Test
  void forwardToSamePathDetectedAsCycle() {
    var handler = createHandlerWithEchoMapping();

    MockHttpContext request = new MockHttpContext();
    request.requestURI = "/a";

    assertThatThrownBy(() -> handler.forward(request, "/a"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Circular forward")
            .hasMessageContaining("/a");
  }

  @Test
  void forwardCycleABCADetected() throws Exception {
    var handler = createHandlerWithEchoMapping();

    MockHttpContext request = new MockHttpContext();
    request.requestURI = "/a";

    handler.forward(request, "/b");
    handler.forward(request, "/c");

    assertThatThrownBy(() -> handler.forward(request, "/a"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Circular forward")
            .hasMessageContaining("/a");
  }

  @Test
  void forwardChainWithoutCycleSucceeds() throws Exception {
    var handler = createHandlerWithEchoMapping();

    MockHttpContext request = new MockHttpContext();
    request.requestURI = "/start";

    handler.forward(request, "/a");
    handler.forward(request, "/b");
    handler.forward(request, "/c");
    handler.forward(request, "/d");

    assertThat(request.requestURI).isEqualTo("/d");
  }

  @Test
  void forwardAfterCommitThrowsIllegalState() {
    var handler = createHandlerWithEchoMapping();

    MockHttpContext request = new MockHttpContext() {
      @Override
      public boolean isCommitted() {
        return true;
      }
    };

    assertThatThrownBy(() -> handler.forward(request, "/any"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("committed");
  }

  @Test
  void forwardSetsForwardRequestUriAttribute() throws Exception {
    var handler = createHandlerWithEchoMapping();

    MockHttpContext request = new MockHttpContext();
    request.requestURI = "/original";

    handler.forward(request, "/forwarded");

    assertThat(request.getAttribute(HttpContext.FORWARD_REQUEST_URI_ATTRIBUTE))
            .isEqualTo("/original");
    assertThat(request.getAttribute(HttpContext.FORWARD_ATTRIBUTE))
            .isEqualTo(Boolean.TRUE);
  }

  @Test
  void forwardToDistinctSamePathInDifferentRequestsNoCycle() throws Exception {
    var handler = createHandlerWithEchoMapping();

    // First request: forward /a -> /b works
    MockHttpContext req1 = new MockHttpContext();
    req1.requestURI = "/a";
    handler.forward(req1, "/b");
    assertThat(req1.requestURI).isEqualTo("/b");

    // Second request: forward /a -> /b also works (different request, no cycle)
    MockHttpContext req2 = new MockHttpContext();
    req2.requestURI = "/a";
    handler.forward(req2, "/b");
    assertThat(req2.requestURI).isEqualTo("/b");
  }

  @Test
  void forwardSelfReForwardNoCycleIfNotSameChain() throws Exception {
    var handler = createHandlerWithEchoMapping();

    // /a -> /b works
    MockHttpContext request = new MockHttpContext();
    request.requestURI = "/a";
    handler.forward(request, "/b");

    // forward /b -> /c
    handler.forward(request, "/c");

    // now /c -> /b is a cycle (b already visited)
    assertThatThrownBy(() -> handler.forward(request, "/b"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Circular forward");
  }

  // -- integration tests through handleRequest() --

  @Test
  void forwardViaHandleRequestWithHttpRequestHandler() throws Throwable {
    var context = createContext();
    var handler = new DispatcherHandler(context);
    handler.setHandlerAdapter(httpRequestHandlerAdapter());

    handler.setHandlerMapping(ctx -> {
      String uri = ctx.getRequestURI();
      if ("/first".equals(uri)) {
        return (HttpRequestHandler) req -> {
          req.forward("/second");
          return HttpRequestHandler.NONE_RETURN_VALUE;
        };
      }
      if ("/second".equals(uri)) {
        return (HttpRequestHandler) req -> {
          req.setAttribute("reachedSecond", Boolean.TRUE);
          return HttpRequestHandler.NONE_RETURN_VALUE;
        };
      }
      return null;
    });
    handler.start();

    MockHttpContext request = new MockHttpContext(
            new MockRequest("GET", "/first"), handler);
    request.setUseForward(true);
    handler.handleRequest(request);

    assertThat(request.requestURI).isEqualTo("/second");
    assertThat(request.getAttribute("reachedSecond")).isEqualTo(Boolean.TRUE);
    assertThat(request.getAttribute(HttpContext.FORWARD_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
  }

  @Test
  void cycleDetectedInIntegrationFlow() throws Throwable {
    var context = createContext();
    var handler = new DispatcherHandler(context);
    handler.setHandlerAdapter(httpRequestHandlerAdapter());

    handler.setHandlerMapping(ctx -> {
      String uri = ctx.getRequestURI();
      if ("/a".equals(uri)) {
        return (HttpRequestHandler) req -> {
          req.forward("/b");
          return HttpRequestHandler.NONE_RETURN_VALUE;
        };
      }
      if ("/b".equals(uri)) {
        return (HttpRequestHandler) req -> {
          req.forward("/a");
          return HttpRequestHandler.NONE_RETURN_VALUE;
        };
      }
      return null;
    });
    handler.start();

    MockHttpContext request = new MockHttpContext(
            new MockRequest("GET", "/a"), handler);
    request.setUseForward(true);
    handler.handleRequest(request);

    // Cycle detected: forward to "/a" did not complete,
    // so the request URI remains at "/b" (the last successful forward target)
    assertThat(request.requestURI).isEqualTo("/b");
    assertThat(request.getAttribute(DispatcherHandler.FORWARDED_PATHS_ATTRIBUTE))
            .as("forwarded paths set should still be present")
            .isNotNull();
  }

  @Test
  void forwardIsNotCommittedAfterChain() throws Exception {
    var handler = createHandlerWithEchoMapping();

    MockHttpContext request = new MockHttpContext();
    request.setUseForward(true);
    request.requestURI = "/a";

    handler.forward(request, "/b");
    handler.forward(request, "/c");

    assertThat(request.isCommitted()).isFalse();
    assertThat(request.requestURI).isEqualTo("/c");
  }

  // -- query string tests --

  @Test
  void forwardWithQueryString() throws Exception {
    var handler = createHandlerWithEchoMapping();

    MockHttpContext request = new MockHttpContext();
    request.requestURI = "/source";

    handler.forward(request, "/target?foo=bar&baz=1");

    assertThat(request.requestURI).isEqualTo("/target");
    assertThat(request.queryString).isEqualTo("foo=bar&baz=1");
    assertThat(request.getRequestURI()).isEqualTo("/target");
    assertThat(request.getQueryString()).isEqualTo("foo=bar&baz=1");
  }

  @Test
  void forwardWithQueryStringDetectsCycle() throws Exception {
    var handler = createHandlerWithEchoMapping();

    MockHttpContext request = new MockHttpContext();
    request.setRequestURI("/a");

    handler.forward(request, "/b?x=1");

    assertThatThrownBy(() -> handler.forward(request, "/a?y=2"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Circular forward")
            .hasMessageContaining("/a");
  }

  @Test
  void forwardWithoutQueryStringClearsIt() throws Exception {
    var handler = createHandlerWithEchoMapping();

    MockHttpContext request = new MockHttpContext();
    request.requestURI = "/source";
    request.queryString = "old=param";

    handler.forward(request, "/target");

    assertThat(request.requestURI).isEqualTo("/target");
    assertThat(request.queryString).isNull();
  }

  @Test
  void forwardResetsParametersAndQueryStringRoundTrips() throws Exception {
    var handler = createHandlerWithEchoMapping();

    MockHttpContext request = new MockHttpContext();
    request.requestURI = "/source";

    handler.forward(request, "/target?foo=bar");

    assertThat(request.requestURI).isEqualTo("/target");
    assertThat(request.queryString).isEqualTo("foo=bar");
    assertThat(request.parameters).isNull();

    // getParameter loads lazily from the new query string
    assertThat(request.getParameter("foo")).isEqualTo("bar");
  }

  @Test
  void forwardResetsParametersPreservesBodyParams() throws Exception {
    var handler = createHandlerWithEchoMapping();

    MockHttpContext request = new MockHttpContext();
    request.requestURI = "/source";
    // Simulate body parameters already parsed and cached
    MultiValueMapAdapter<String, String> formUrlencoded = new MultiValueMapAdapter<>(new HashMap<>());
    request.setFormUrlencoded(formUrlencoded);
    formUrlencoded.add("bodyParam", "bodyValue");

    handler.forward(request, "/target?q=search");

    assertThat(request.parameters).isNull();

    // On lazy re-read: new query string + cached body params
    assertThat(request.getParameter("q")).isEqualTo("search");
    assertThat(request.getParameter("bodyParam")).isEqualTo("bodyValue");
  }

  // -- helpers --

  private static AnnotationConfigApplicationContext createContext() {
    var context = new AnnotationConfigApplicationContext();
    context.register(BaseConfig.class);
    context.refresh();
    return context;
  }

  /**
   * Creates a handler whose mapping returns an {@link HttpRequestHandler} that
   * simply echoes {@link HttpRequestHandler#NONE_RETURN_VALUE} for any path,
   * preventing the not-found handler from committing the response.
   */
  private static DispatcherHandler createHandlerWithEchoMapping() {
    var context = createContext();
    var handler = new DispatcherHandler(context);
    handler.setHandlerAdapter(httpRequestHandlerAdapter());
    handler.setHandlerMapping(ctx -> (HttpRequestHandler) req -> HttpRequestHandler.NONE_RETURN_VALUE);
    handler.start();
    return handler;
  }

  private static HandlerAdapter httpRequestHandlerAdapter() {
    return new HandlerAdapter() {
      @Override
      public boolean supports(Object handler) {
        return handler instanceof HttpRequestHandler;
      }

      @Override
      public Object handle(HttpContext request, Object handler) throws Throwable {
        return ((HttpRequestHandler) handler).handleRequest(request);
      }
    };
  }

}
