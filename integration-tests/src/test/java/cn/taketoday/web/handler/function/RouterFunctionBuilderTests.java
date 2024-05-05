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

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.CacheControl;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.view.PathPatternsTestUtils;

import static cn.taketoday.web.handler.function.RequestPredicates.HEAD;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RouterFunctionBuilder}.
 *
 * @author Arjen Poutsma
 */
class RouterFunctionBuilderTests {

  @Test
  void route() {
    RouterFunction<ServerResponse> route = RouterFunctions.route()
            .GET("/foo", request -> ServerResponse.ok().build())
            .POST("/", RequestPredicates.contentType(MediaType.TEXT_PLAIN),
                    request -> ServerResponse.noContent().build())
            .route(HEAD("/foo"), request -> ServerResponse.accepted().build())
            .build();

    ServerRequest getFooRequest = initRequest("GET", "/foo");

    Optional<HttpStatusCode> responseStatus = route.route(getFooRequest)
            .map(handlerFunction -> handle(handlerFunction, getFooRequest))
            .map(ServerResponse::statusCode);
    assertThat(responseStatus).contains(HttpStatus.OK);

    ServerRequest headFooRequest = initRequest("HEAD", "/foo");

    responseStatus = route.route(headFooRequest)
            .map(handlerFunction -> handle(handlerFunction, getFooRequest))
            .map(ServerResponse::statusCode);
    assertThat(responseStatus).contains(HttpStatus.ACCEPTED);

    ServerRequest barRequest = initRequest("POST", "/", req -> req.setContentType("text/plain"));

    responseStatus = route.route(barRequest)
            .map(handlerFunction -> handle(handlerFunction, barRequest))
            .map(ServerResponse::statusCode);
    assertThat(responseStatus).contains(HttpStatus.NO_CONTENT);

    ServerRequest invalidRequest = initRequest("POST", "/");

    responseStatus = route.route(invalidRequest)
            .map(handlerFunction -> handle(handlerFunction, invalidRequest))
            .map(ServerResponse::statusCode);

    assertThat(responseStatus).isEmpty();
  }

  private static ServerResponse handle(HandlerFunction<ServerResponse> handlerFunction,
          ServerRequest request) {
    try {
      return handlerFunction.handle(request);
    }
    catch (Exception ex) {
      throw new AssertionError(ex.getMessage(), ex);
    }
  }

  @Test
  void resource() {
    Resource resource = new ClassPathResource("/cn/taketoday/web/handler/function/response.txt");
    assertThat(resource.exists()).isTrue();

    RouterFunction<ServerResponse> route = RouterFunctions.route()
            .resource(RequestPredicates.path("/test"), resource)
            .build();

    ServerRequest resourceRequest = initRequest("GET", "/test");

    Optional<HttpStatusCode> responseStatus = route.route(resourceRequest)
            .map(handlerFunction -> handle(handlerFunction, resourceRequest))
            .map(ServerResponse::statusCode);
    assertThat(responseStatus).contains(HttpStatus.OK);
  }

  @Test
  void resources() {
    Resource resource = new ClassPathResource("/cn/taketoday/web/handler/function/");
    assertThat(resource.exists()).isTrue();

    RouterFunction<ServerResponse> route = RouterFunctions.route()
            .resources("/resources/**", resource)
            .build();

    ServerRequest resourceRequest = initRequest("GET", "/resources/response.txt");

    Optional<HttpStatusCode> responseStatus = route.route(resourceRequest)
            .map(handlerFunction -> handle(handlerFunction, resourceRequest))
            .map(ServerResponse::statusCode);
    assertThat(responseStatus).contains(HttpStatus.OK);

    ServerRequest invalidRequest = initRequest("POST", "/resources/foo.txt");

    responseStatus = route.route(invalidRequest)
            .map(handlerFunction -> handle(handlerFunction, invalidRequest))
            .map(ServerResponse::statusCode);
    assertThat(responseStatus).isEmpty();
  }

  @Test
  public void resourcesCaching() {
    Resource resource = new ClassPathResource("/cn/taketoday/web/handler/function/");
    assertThat(resource.exists()).isTrue();

    RouterFunction<ServerResponse> route = RouterFunctions.route()
            .resources("/resources/**", resource, (r, headers) -> headers.setCacheControl(CacheControl.maxAge(Duration.ofSeconds(60))))
            .build();

    ServerRequest resourceRequest = initRequest("GET", "/resources/response.txt");

    Optional<String> responseCacheControl = route.route(resourceRequest)
            .map(handlerFunction -> handle(handlerFunction, resourceRequest))
            .map(response -> response.headers().getCacheControl());
    assertThat(responseCacheControl).contains("max-age=60");
  }

  @Test
  void nest() {
    RouterFunction<ServerResponse> route = RouterFunctions.route()
            .path("/foo", builder ->
                    builder.path("/bar",
                            () -> RouterFunctions.route()
                                    .GET("/baz", request -> ServerResponse.ok().build())
                                    .build()))
            .build();

    ServerRequest fooRequest = initRequest("GET", "/foo/bar/baz");

    Optional<HttpStatusCode> responseStatus = route.route(fooRequest)
            .map(handlerFunction -> handle(handlerFunction, fooRequest))
            .map(ServerResponse::statusCode);
    assertThat(responseStatus).contains(HttpStatus.OK);
  }

  @Test
  void filters() {
    AtomicInteger filterCount = new AtomicInteger();

    RouterFunction<ServerResponse> route = RouterFunctions.route()
            .GET("/foo", request -> ServerResponse.ok().build())
            .GET("/bar", request -> {
              throw new IllegalStateException();
            })
            .before(request -> {
              int count = filterCount.getAndIncrement();
              assertThat(count).isEqualTo(0);
              return request;
            })
            .after((request, response) -> {
              int count = filterCount.getAndIncrement();
              assertThat(count).isEqualTo(3);
              return response;
            })
            .filter((request, next) -> {
              int count = filterCount.getAndIncrement();
              assertThat(count).isEqualTo(1);
              ServerResponse responseMono = next.handle(request);
              count = filterCount.getAndIncrement();
              assertThat(count).isEqualTo(2);
              return responseMono;
            })
            .onError(IllegalStateException.class,
                    (e, request) -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build())
            .build();

    ServerRequest fooRequest = initRequest("GET", "/foo");

    route.route(fooRequest)
            .map(handlerFunction -> handle(handlerFunction, fooRequest));
    assertThat(filterCount.get()).isEqualTo(4);

    filterCount.set(0);

    ServerRequest barRequest = initRequest("GET", "/bar");

    Optional<HttpStatusCode> responseStatus = route.route(barRequest)
            .map(handlerFunction -> handle(handlerFunction, barRequest))
            .map(ServerResponse::statusCode);
    assertThat(responseStatus).contains(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  public void multipleOnErrors() {
    RouterFunction<ServerResponse> route = RouterFunctions.route()
            .GET("/error", request -> {
              throw new IOException();
            })
            .onError(IOException.class, (t, r) -> ServerResponse.status(200).build())
            .onError(Exception.class, (t, r) -> ServerResponse.status(201).build())
            .build();

    HttpMockRequestImpl servletRequest = new HttpMockRequestImpl("GET", "/error");

    MockHttpResponseImpl servletResponse = new MockHttpResponseImpl();
    var requestContext = new MockRequestContext(null, servletRequest, servletResponse);

    ServerRequest serverRequest = new DefaultServerRequest(requestContext, emptyList());

    Optional<HttpStatusCode> responseStatus = route.route(serverRequest)
            .map(handlerFunction -> handle(handlerFunction, serverRequest))
            .map(ServerResponse::statusCode);
    assertThat(responseStatus).contains(HttpStatus.OK);

  }

  private ServerRequest initRequest(String httpMethod, String requestUri) {
    return initRequest(httpMethod, requestUri, null);
  }

  private ServerRequest initRequest(
          String httpMethod, String requestUri, @Nullable Consumer<HttpMockRequestImpl> consumer) {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest(httpMethod, null, requestUri, true, consumer);
    MockHttpResponseImpl servletResponse = new MockHttpResponseImpl();
    var requestContext = new MockRequestContext(null, servletRequest, servletResponse);
    return new DefaultServerRequest(requestContext, emptyList());
  }

  @Test
  public void attributes() {
    RouterFunction<ServerResponse> route = RouterFunctions.route()
            .GET("/atts/1", request -> ServerResponse.ok().build())
            .withAttribute("foo", "bar")
            .withAttribute("baz", "qux")
            .GET("/atts/2", request -> ServerResponse.ok().build())
            .withAttributes(atts -> {
              atts.put("foo", "bar");
              atts.put("baz", "qux");
            })
            .path("/atts", b1 -> b1
                    .GET("/3", request -> ServerResponse.ok().build())
                    .withAttribute("foo", "bar")
                    .GET("/4", request -> ServerResponse.ok().build())
                    .withAttribute("baz", "qux")
                    .path("/5", b2 -> b2
                            .GET(request -> ServerResponse.ok().build())
                            .withAttribute("foo", "n3"))
                    .withAttribute("foo", "n2")
            )
            .withAttribute("foo", "n1")
            .build();

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
}
