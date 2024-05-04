/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.handler.function;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.view.PathPatternsTestUtils;

import static cn.taketoday.http.HttpMethod.GET;
import static cn.taketoday.web.handler.function.RequestPredicates.GET;
import static cn.taketoday.web.handler.function.RequestPredicates.method;
import static cn.taketoday.web.handler.function.RequestPredicates.path;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class RouterFunctionTests {

  MockHttpServletResponse servletResponse = new MockHttpServletResponse();

  private final ServerRequest request = new DefaultServerRequest(
          new ServletRequestContext(null, PathPatternsTestUtils.initRequest("GET", "", true), servletResponse), Collections.emptyList());

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

  private ServerResponse handlerMethod(ServerRequest request) {
    return ServerResponse.ok().body("42");
  }

}
