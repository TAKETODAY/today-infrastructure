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

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import infra.http.HttpStatus;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.mock.MockRequestContext;
import infra.web.view.PathPatternsTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
public class RouterFunctionsTests {

  MockHttpResponseImpl mockResponse = new MockHttpResponseImpl();

  private final ServerRequest request = new DefaultServerRequest(
          new MockRequestContext(null, PathPatternsTestUtils.initRequest("GET", "", true), mockResponse), Collections.emptyList());

  @Test
  public void routeMatch() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();

    RequestPredicate requestPredicate = mock(RequestPredicate.class);
    given(requestPredicate.test(request)).willReturn(true);

    RouterFunction<ServerResponse>
            result = RouterFunctions.route(requestPredicate, handlerFunction);
    assertThat(result).isNotNull();

    Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isTrue();
    assertThat(resultHandlerFunction.get()).isEqualTo(handlerFunction);
  }

  @Test
  public void routeNoMatch() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();

    RequestPredicate requestPredicate = mock(RequestPredicate.class);
    given(requestPredicate.test(request)).willReturn(false);

    RouterFunction<ServerResponse> result = RouterFunctions.route(requestPredicate, handlerFunction);
    assertThat(result).isNotNull();

    Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isFalse();
  }

  @Test
  public void nestMatch() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RouterFunction<ServerResponse> routerFunction = request -> Optional.of(handlerFunction);

    RequestPredicate requestPredicate = mock(RequestPredicate.class);
    given(requestPredicate.nest(request)).willReturn(Optional.of(request));

    RouterFunction<ServerResponse> result = RouterFunctions.nest(requestPredicate, routerFunction);
    assertThat(result).isNotNull();

    Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isTrue();
    assertThat(resultHandlerFunction.get()).isEqualTo(handlerFunction);
  }

  @Test
  public void nestNoMatch() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RouterFunction<ServerResponse> routerFunction = request -> Optional.of(handlerFunction);

    RequestPredicate requestPredicate = mock(RequestPredicate.class);
    given(requestPredicate.nest(request)).willReturn(Optional.empty());

    RouterFunction<ServerResponse> result = RouterFunctions.nest(requestPredicate, routerFunction);
    assertThat(result).isNotNull();

    Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isFalse();
  }

  @Test
  public void nestPathVariable() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RequestPredicate requestPredicate = request -> request.pathVariable("foo").equals("bar");
    RouterFunction<ServerResponse> nestedFunction = RouterFunctions.route(requestPredicate, handlerFunction);

    RouterFunction<ServerResponse> result = RouterFunctions.nest(RequestPredicates.path("/{foo}"), nestedFunction);
    assertThat(result).isNotNull();

    HttpMockRequestImpl servletRequest = new HttpMockRequestImpl("GET", "/bar");

    MockHttpResponseImpl servletResponse = new MockHttpResponseImpl();
    var requestContext = new MockRequestContext(null, servletRequest, servletResponse);
    ServerRequest request = new DefaultServerRequest(requestContext, Collections.emptyList());

    Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isTrue();
    assertThat(resultHandlerFunction.get()).isEqualTo(handlerFunction);
  }

  @Test
  public void builderGet() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
            .GET(handlerFunction)
            .build();

    ServerRequest getRequest = new DefaultServerRequest(
            new MockRequestContext(null, PathPatternsTestUtils.initRequest("GET", "/", true), mockResponse),
            Collections.emptyList());

    Optional<HandlerFunction<ServerResponse>> result = routerFunction.route(getRequest);
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isEqualTo(handlerFunction);
  }

  @Test
  public void builderGetWithPattern() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
            .GET("/test", handlerFunction)
            .build();

    ServerRequest matchingRequest = new DefaultServerRequest(
            new MockRequestContext(null, PathPatternsTestUtils.initRequest("GET", "/test", true), mockResponse),
            Collections.emptyList());

    ServerRequest nonMatchingRequest = new DefaultServerRequest(
            new MockRequestContext(null, PathPatternsTestUtils.initRequest("GET", "/other", true), mockResponse),
            Collections.emptyList());

    Optional<HandlerFunction<ServerResponse>> matchingResult = routerFunction.route(matchingRequest);
    assertThat(matchingResult.isPresent()).isTrue();
    assertThat(matchingResult.get()).isEqualTo(handlerFunction);

    Optional<HandlerFunction<ServerResponse>> nonMatchingResult = routerFunction.route(nonMatchingRequest);
    assertThat(nonMatchingResult.isPresent()).isFalse();
  }

  @Test
  public void builderRoute() {
    RequestPredicate predicate = RequestPredicates.path("/route");
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();

    RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
            .route(predicate, handlerFunction)
            .build();

    ServerRequest matchingRequest = new DefaultServerRequest(
            new MockRequestContext(null, PathPatternsTestUtils.initRequest("GET", "/route", true), mockResponse),
            Collections.emptyList());

    ServerRequest nonMatchingRequest = new DefaultServerRequest(
            new MockRequestContext(null, PathPatternsTestUtils.initRequest("GET", "/other", true), mockResponse),
            Collections.emptyList());

    Optional<HandlerFunction<ServerResponse>> matchingResult = routerFunction.route(matchingRequest);
    assertThat(matchingResult.isPresent()).isTrue();
    assertThat(matchingResult.get()).isEqualTo(handlerFunction);

    Optional<HandlerFunction<ServerResponse>> nonMatchingResult = routerFunction.route(nonMatchingRequest);
    assertThat(nonMatchingResult.isPresent()).isFalse();
  }

  @Test
  public void builderNest() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();

    RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
            .nest(RequestPredicates.path("/api"), () -> RouterFunctions.route()
                    .GET("/users", handlerFunction)
                    .build())
            .build();

    ServerRequest matchingRequest = new DefaultServerRequest(
            new MockRequestContext(null, PathPatternsTestUtils.initRequest("GET", "/api/users", true), mockResponse),
            Collections.emptyList());

    ServerRequest nonMatchingRequest = new DefaultServerRequest(
            new MockRequestContext(null, PathPatternsTestUtils.initRequest("GET", "/users", true), mockResponse),
            Collections.emptyList());

    Optional<HandlerFunction<ServerResponse>> matchingResult = routerFunction.route(matchingRequest);
    assertThat(matchingResult.isPresent()).isTrue();
    assertThat(matchingResult.get()).isEqualTo(handlerFunction);

    Optional<HandlerFunction<ServerResponse>> nonMatchingResult = routerFunction.route(nonMatchingRequest);
    assertThat(nonMatchingResult.isPresent()).isFalse();
  }

  @Test
  public void builderPath() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();

    RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
            .path("/api", builder -> builder.GET("/users", handlerFunction))
            .build();

    ServerRequest matchingRequest = new DefaultServerRequest(
            new MockRequestContext(null, PathPatternsTestUtils.initRequest("GET", "/api/users", true), mockResponse),
            Collections.emptyList());

    ServerRequest nonMatchingRequest = new DefaultServerRequest(
            new MockRequestContext(null, PathPatternsTestUtils.initRequest("GET", "/users", true), mockResponse),
            Collections.emptyList());

    Optional<HandlerFunction<ServerResponse>> matchingResult = routerFunction.route(matchingRequest);
    assertThat(matchingResult.isPresent()).isTrue();
    assertThat(matchingResult.get()).isEqualTo(handlerFunction);

    Optional<HandlerFunction<ServerResponse>> nonMatchingResult = routerFunction.route(nonMatchingRequest);
    assertThat(nonMatchingResult.isPresent()).isFalse();
  }

  @Test
  public void builderBeforeFilter() {
    HandlerFunction<ServerResponse> handlerFunction = request -> {
      String attr = (String) request.attribute("processed");
      return ServerResponse.ok().body(attr != null ? attr : "not processed");
    };

    RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
            .GET("/test", handlerFunction)
            .before(request -> {
              Map<String, Object> attributes = new LinkedHashMap<>(request.attributes());
              attributes.put("processed", "before");
              return ServerRequest.from(request).attributes(map -> {
                map.putAll(attributes);
              }).build();
            })
            .build();

    ServerRequest testRequest = new DefaultServerRequest(
            new MockRequestContext(null, PathPatternsTestUtils.initRequest("GET", "/test", true), mockResponse),
            Collections.emptyList());

    Optional<HandlerFunction<ServerResponse>> result = routerFunction.route(testRequest);
    assertThat(result.isPresent()).isTrue();

    try {
      ServerResponse response = result.get().handle(testRequest);
      assertThat(response).isInstanceOf(EntityResponse.class);
      EntityResponse<String> entityResponse = (EntityResponse<String>) response;
      assertThat(entityResponse.entity()).isEqualTo("before");
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void builderAfterFilter() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().body("original");

    RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
            .GET("/test", handlerFunction)
            .after((request, response) -> ServerResponse.ok().body("modified"))
            .build();

    ServerRequest testRequest = new DefaultServerRequest(
            new MockRequestContext(null, PathPatternsTestUtils.initRequest("GET", "/test", true), mockResponse),
            Collections.emptyList());

    Optional<HandlerFunction<ServerResponse>> result = routerFunction.route(testRequest);
    assertThat(result.isPresent()).isTrue();

    try {
      ServerResponse response = result.get().handle(testRequest);
      assertThat(response).isInstanceOf(EntityResponse.class);
      EntityResponse<String> entityResponse = (EntityResponse<String>) response;
      assertThat(entityResponse.entity()).isEqualTo("modified");
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void builderOnError() {
    HandlerFunction<ServerResponse> handlerFunction = request -> {
      throw new IllegalStateException("test exception");
    };

    RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
            .GET("/test", handlerFunction)
            .onError(IllegalStateException.class, (e, request) -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()))
            .build();

    ServerRequest testRequest = new DefaultServerRequest(
            new MockRequestContext(null, PathPatternsTestUtils.initRequest("GET", "/test", true), mockResponse),
            Collections.emptyList());

    Optional<HandlerFunction<ServerResponse>> result = routerFunction.route(testRequest);
    assertThat(result.isPresent()).isTrue();

    try {
      ServerResponse response = result.get().handle(testRequest);
      assertThat(response).isInstanceOf(EntityResponse.class);
      EntityResponse<String> entityResponse = (EntityResponse<String>) response;
      assertThat(entityResponse.entity()).isEqualTo("test exception");
      assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
