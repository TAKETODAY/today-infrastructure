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

package infra.web.handler.function;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import infra.mock.web.MockHttpResponseImpl;
import infra.web.mock.MockRequestContext;

import static infra.http.HttpMethod.GET;
import static infra.web.handler.function.RequestPredicates.GET;
import static infra.web.handler.function.RequestPredicates.method;
import static infra.web.handler.function.RequestPredicates.path;
import static infra.web.view.PathPatternsTestUtils.initRequest;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class RouterFunctionTests {

  MockHttpResponseImpl mockResponse = new MockHttpResponseImpl();

  private final ServerRequest request = new DefaultServerRequest(
          new MockRequestContext(null, initRequest("GET", "", true), mockResponse), Collections.emptyList());

  @Test
  void and() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RouterFunction<ServerResponse> routerFunction1 = request -> Optional.empty();
    RouterFunction<ServerResponse> routerFunction2 = request -> Optional.of(handlerFunction);

    RouterFunction<ServerResponse> result = routerFunction1.and(routerFunction2);
    assertThat(result).isNotNull();

    Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isTrue();
    assertThat(resultHandlerFunction.get()).isEqualTo(handlerFunction);
  }

  @Test
  void andOther() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().body("42");
    RouterFunction<?> routerFunction1 = request -> Optional.empty();
    RouterFunction<ServerResponse> routerFunction2 = request -> Optional.of(handlerFunction);

    RouterFunction<?> result = routerFunction1.andOther(routerFunction2);
    assertThat(result).isNotNull();

    Optional<? extends HandlerFunction<?>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isTrue();
    assertThat(resultHandlerFunction.get()).isEqualTo(handlerFunction);
  }

  @Test
  void andRoute() {
    RouterFunction<ServerResponse> routerFunction1 = request -> Optional.empty();
    RequestPredicate requestPredicate = request -> true;

    RouterFunction<ServerResponse> result = routerFunction1.andRoute(requestPredicate, this::handlerMethod);
    assertThat(result).isNotNull();

    Optional<? extends HandlerFunction<?>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isTrue();
  }

  @Test
  void filter() {
    String string = "42";
    HandlerFunction<EntityResponse<String>> handlerFunction =
            request -> EntityResponse.fromObject(string).build();
    RouterFunction<EntityResponse<String>> routerFunction =
            request -> Optional.of(handlerFunction);

    HandlerFilterFunction<EntityResponse<String>, EntityResponse<Integer>> filterFunction =
            (request, next) -> {
              String stringResponse = next.handle(request).entity();
              Integer intResponse = Integer.parseInt(stringResponse);
              return EntityResponse.fromObject(intResponse).build();
            };

    RouterFunction<EntityResponse<Integer>> result = routerFunction.filter(filterFunction);
    assertThat(result).isNotNull();

    Optional<EntityResponse<Integer>> resultHandlerFunction = result.route(request)
            .map(hf -> {
              try {
                return hf.handle(request);
              }
              catch (Exception ex) {
                throw new AssertionError(ex.getMessage(), ex);
              }
            });
    assertThat(resultHandlerFunction.isPresent()).isTrue();
    assertThat((int) resultHandlerFunction.get().entity()).isEqualTo(42);
  }

  @Test
  public void attributes() {
    RouterFunction<ServerResponse> route = RouterFunctions.route(
                    GET("/atts/1"), request -> ServerResponse.ok().build())
            .withAttribute("foo", "bar")
            .withAttribute("baz", "qux")
            .and(RouterFunctions.route(GET("/atts/2"), request -> ServerResponse.ok().build())
                    .withAttributes(atts -> {
                      atts.put("foo", "bar");
                      atts.put("baz", "qux");
                    }))
            .and(RouterFunctions.nest(path("/atts"),
                            RouterFunctions.route(GET("/3"), request -> ServerResponse.ok().build())
                                    .withAttribute("foo", "bar")
                                    .and(RouterFunctions.route(GET("/4"), request -> ServerResponse.ok().build())
                                            .withAttribute("baz", "qux"))
                                    .and(RouterFunctions.nest(path("/5"),
                                                    RouterFunctions.route(method(GET), request -> ServerResponse.ok().build())
                                                            .withAttribute("foo", "n3"))
                                            .withAttribute("foo", "n2")))
                    .withAttribute("foo", "n1"));

    AttributesTestVisitor visitor = new AttributesTestVisitor();
    route.accept(visitor);
    Assertions.assertThat(visitor.routerFunctionsAttributes()).containsExactly(
            List.of(Map.of("foo", "bar", "baz", "qux")),
            List.of(Map.of("foo", "bar", "baz", "qux")),
            List.of(Map.of("foo", "bar"), Map.of("foo", "n1")),
            List.of(Map.of("baz", "qux"), Map.of("foo", "n1")),
            List.of(Map.of("foo", "n3"), Map.of("foo", "n2"), Map.of("foo", "n1"))
    );
    Assertions.assertThat(visitor.visitCount()).isEqualTo(7);
  }

  @Test
  void withAttribute() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RouterFunction<ServerResponse> routerFunction = RouterFunctions.route(GET("/test"), handlerFunction);

    RouterFunction<ServerResponse> result = routerFunction.withAttribute("key", "value");
    assertThat(result).isNotNull();

    AttributesTestVisitor visitor = new AttributesTestVisitor();
    result.accept(visitor);
    assertThat(visitor.visitCount()).isEqualTo(1);
  }

  @Test
  void withAttributes() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RouterFunction<ServerResponse> routerFunction = RouterFunctions.route(GET("/test"), handlerFunction);

    RouterFunction<ServerResponse> result = routerFunction.withAttributes(attributes -> {
      attributes.put("key1", "value1");
      attributes.put("key2", "value2");
    });
    assertThat(result).isNotNull();

    AttributesTestVisitor visitor = new AttributesTestVisitor();
    result.accept(visitor);
    assertThat(visitor.visitCount()).isEqualTo(1);
  }

  @Test
  void filterWithMultipleFilters() {
    HandlerFunction<EntityResponse<String>> handlerFunction = request -> EntityResponse.fromObject("42").build();
    RouterFunction<EntityResponse<String>> routerFunction = request -> Optional.of(handlerFunction);

    HandlerFilterFunction<EntityResponse<String>, EntityResponse<Integer>> filterFunction1 =
            (request, next) -> {
              String response = next.handle(request).entity();
              Integer intResponse = Integer.parseInt(response);
              return EntityResponse.fromObject(intResponse).build();
            };

    HandlerFilterFunction<EntityResponse<Integer>, EntityResponse<Double>> filterFunction2 =
            (request, next) -> {
              Integer response = next.handle(request).entity();
              Double doubleResponse = response.doubleValue();
              return EntityResponse.fromObject(doubleResponse).build();
            };

    RouterFunction<EntityResponse<Double>> result = routerFunction
            .filter(filterFunction1)
            .filter(filterFunction2);

    assertThat(result).isNotNull();

    Optional<EntityResponse<Double>> resultHandlerFunction = result.route(request)
            .map(hf -> {
              try {
                return hf.handle(request);
              }
              catch (Exception ex) {
                throw new AssertionError(ex.getMessage(), ex);
              }
            });
    assertThat(resultHandlerFunction.isPresent()).isTrue();
    assertThat(resultHandlerFunction.get().entity()).isEqualTo(42.0);
  }

  @Test
  void andWithMultipleRoutes() {
    HandlerFunction<ServerResponse> handlerFunction1 = request -> ServerResponse.ok().body("route1");
    HandlerFunction<ServerResponse> handlerFunction2 = request -> ServerResponse.ok().body("route2");
    HandlerFunction<ServerResponse> handlerFunction3 = request -> ServerResponse.ok().body("route3");

    RouterFunction<ServerResponse> routerFunction1 = RouterFunctions.route(GET("/route1"), handlerFunction1);
    RouterFunction<ServerResponse> routerFunction2 = RouterFunctions.route(GET("/route2"), handlerFunction2);
    RouterFunction<ServerResponse> routerFunction3 = RouterFunctions.route(GET("/route3"), handlerFunction3);

    RouterFunction<ServerResponse> result = routerFunction1
            .and(routerFunction2)
            .and(routerFunction3);

    assertThat(result).isNotNull();

    // Test first route
    ServerRequest request1 = new DefaultServerRequest(
            new MockRequestContext(null, initRequest("GET", "/route1", true), mockResponse),
            Collections.emptyList());
    Optional<HandlerFunction<ServerResponse>> handler1 = result.route(request1);
    assertThat(handler1.isPresent()).isTrue();

    // Test second route
    ServerRequest request2 = new DefaultServerRequest(
            new MockRequestContext(null, initRequest("GET", "/route2", true), mockResponse),
            Collections.emptyList());
    Optional<HandlerFunction<ServerResponse>> handler2 = result.route(request2);
    assertThat(handler2.isPresent()).isTrue();

    // Test third route
    ServerRequest request3 = new DefaultServerRequest(
            new MockRequestContext(null, initRequest("GET", "/route3", true), mockResponse),
            Collections.emptyList());
    Optional<HandlerFunction<ServerResponse>> handler3 = result.route(request3);
    assertThat(handler3.isPresent()).isTrue();
  }

  @Test
  void andOtherWithDifferentResponseTypes() {
    HandlerFunction<EntityResponse<String>> stringHandler = request -> EntityResponse.fromObject("string").build();
    HandlerFunction<EntityResponse<Integer>> intHandler = request -> EntityResponse.fromObject(42).build();

    RouterFunction<EntityResponse<String>> routerFunction1 = request -> Optional.empty();
    RouterFunction<EntityResponse<Integer>> routerFunction2 = request -> Optional.of(intHandler);

    RouterFunction<?> result = routerFunction1.andOther(routerFunction2);
    assertThat(result).isNotNull();

    Optional<? extends HandlerFunction<?>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isTrue();
  }

  @Test
  void routeReturnsEmpty() {
    RouterFunction<ServerResponse> routerFunction = request -> Optional.empty();

    Optional<HandlerFunction<ServerResponse>> result = routerFunction.route(request);
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  void andRouteWithPredicateThatDoesNotMatch() {
    RouterFunction<ServerResponse> routerFunction1 = request -> Optional.empty();
    RequestPredicate requestPredicate = request -> false;

    RouterFunction<ServerResponse> result = routerFunction1.andRoute(requestPredicate, this::handlerMethod);
    assertThat(result).isNotNull();

    Optional<? extends HandlerFunction<?>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isFalse();
  }

  @Test
  void filterWithExceptionHandling() {
    HandlerFunction<EntityResponse<String>> handlerFunction = request -> {
      throw new RuntimeException("Handler error");
    };
    RouterFunction<EntityResponse<String>> routerFunction = request -> Optional.of(handlerFunction);

    HandlerFilterFunction<EntityResponse<String>, EntityResponse<String>> filterFunction =
            (request, next) -> {
              try {
                return next.handle(request);
              }
              catch (Exception ex) {
                return EntityResponse.fromObject("error-handled").build();
              }
            };

    RouterFunction<EntityResponse<String>> result = routerFunction.filter(filterFunction);
    assertThat(result).isNotNull();

    Optional<EntityResponse<String>> resultHandlerFunction = result.route(request)
            .map(hf -> {
              try {
                return hf.handle(request);
              }
              catch (Exception ex) {
                throw new AssertionError(ex.getMessage(), ex);
              }
            });
    assertThat(resultHandlerFunction.isPresent()).isTrue();
    assertThat(resultHandlerFunction.get().entity()).isEqualTo("error-handled");
  }

  @Test
  void withAttributeAndRoute() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RouterFunction<ServerResponse> routerFunction = RouterFunctions.route(GET("/test"), handlerFunction)
            .withAttribute("test-key", "test-value");

    RouterFunction<ServerResponse> result = routerFunction.andRoute(GET("/test2"), request -> ServerResponse.ok().body("test2"))
            .withAttribute("test-key2", "test-value2");

    assertThat(result).isNotNull();

    AttributesTestVisitor visitor = new AttributesTestVisitor();
    result.accept(visitor);
    assertThat(visitor.visitCount()).isEqualTo(2);
  }

  @Test
  void filterThenAnd() {
    HandlerFunction<EntityResponse<String>> handlerFunction1 = request -> EntityResponse.fromObject("handler1").build();
    HandlerFunction<EntityResponse<String>> handlerFunction2 = request -> EntityResponse.fromObject("handler2").build();

    RouterFunction<EntityResponse<String>> routerFunction1 = RouterFunctions.route(GET("/filter"), handlerFunction1)
            .filter((request, next) -> {
              EntityResponse<String> response = next.handle(request);
              return EntityResponse.fromObject("filtered-" + response.entity()).build();
            });

    RouterFunction<EntityResponse<String>> routerFunction2 = RouterFunctions.route(GET("/second"), handlerFunction2);

    RouterFunction<EntityResponse<String>> result = routerFunction1.and(routerFunction2);

    // Test filtered route
    ServerRequest request1 = new DefaultServerRequest(
            new MockRequestContext(null, initRequest("GET", "/filter", true), mockResponse),
            Collections.emptyList());

    Optional<EntityResponse<String>> response1 = result.route(request1)
            .map(hf -> {
              try {
                return hf.handle(request1);
              }
              catch (Exception ex) {
                throw new AssertionError(ex.getMessage(), ex);
              }
            });

    assertThat(response1.isPresent()).isTrue();
    assertThat(response1.get().entity()).isEqualTo("filtered-handler1");

    // Test second route
    ServerRequest request2 = new DefaultServerRequest(
            new MockRequestContext(null, initRequest("GET", "/second", true), mockResponse),
            Collections.emptyList());

    Optional<EntityResponse<String>> response2 = result.route(request2)
            .map(hf -> {
              try {
                return hf.handle(request2);
              }
              catch (Exception ex) {
                throw new AssertionError(ex.getMessage(), ex);
              }
            });

    assertThat(response2.isPresent()).isTrue();
    assertThat(response2.get().entity()).isEqualTo("handler2");
  }

  @Test
  void andOtherWithSameType() {
    HandlerFunction<ServerResponse> handlerFunction1 = request -> ServerResponse.ok().body("first");
    HandlerFunction<ServerResponse> handlerFunction2 = request -> ServerResponse.ok().body("second");

    RouterFunction<ServerResponse> routerFunction1 = request -> Optional.empty();
    RouterFunction<ServerResponse> routerFunction2 = request -> Optional.of(handlerFunction2);

    RouterFunction<?> result = routerFunction1.andOther(routerFunction2);
    assertThat(result).isNotNull();

    Optional<? extends HandlerFunction<?>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isTrue();
    assertThat(resultHandlerFunction.get()).isEqualTo(handlerFunction2);
  }

  @Test
  void multipleAttributes() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RouterFunction<ServerResponse> routerFunction = RouterFunctions.route(GET("/multi-attr"), handlerFunction);

    RouterFunction<ServerResponse> result = routerFunction
            .withAttribute("attr1", "value1")
            .withAttribute("attr2", "value2")
            .withAttributes(attributes -> {
              attributes.put("attr3", "value3");
              attributes.put("attr4", "value4");
            });

    assertThat(result).isNotNull();

    AttributesTestVisitor visitor = new AttributesTestVisitor();
    result.accept(visitor);
    assertThat(visitor.visitCount()).isEqualTo(1);
  }

  @Test
  void andNestWithAttributes() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RequestPredicate predicate = RequestPredicates.path("/nested/**");
    RouterFunction<ServerResponse> nestedRoute = RouterFunctions.route(RequestPredicates.path("/path"), handlerFunction)
            .withAttribute("nested-attr", "nested-value");

    RouterFunction<ServerResponse> routerFunction1 = request -> Optional.empty();
    RouterFunction<ServerResponse> result = routerFunction1.andNest(predicate, nestedRoute)
            .withAttribute("parent-attr", "parent-value");

    assertThat(result).isNotNull();

    AttributesTestVisitor visitor = new AttributesTestVisitor();
    result.accept(visitor);
    assertThat(visitor.visitCount()).isEqualTo(2);
  }

  private ServerResponse handlerMethod(ServerRequest request) {
    return ServerResponse.ok().body("42");
  }

}
