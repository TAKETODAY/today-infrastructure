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

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.lang.Nullable;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.accept.ApiVersionStrategy;
import infra.web.accept.DefaultApiVersionStrategy;
import infra.web.accept.SemanticApiVersionParser;
import infra.web.mock.MockRequestContext;
import infra.web.util.pattern.PathPatternParser;
import infra.web.view.PathPatternsTestUtils;

import static infra.http.MediaType.TEXT_XML_VALUE;
import static infra.web.HandlerMapping.API_VERSION_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class RequestPredicatesTests {

  @Test
  void all() {
    RequestPredicate predicate = RequestPredicates.all();
    ServerRequest request = initRequest("GET", "/");
    assertThat(predicate.test(request)).isTrue();
  }

  @Test
  void method() {
    HttpMethod httpMethod = HttpMethod.GET;
    RequestPredicate predicate = RequestPredicates.method(httpMethod);

    assertThat(predicate.test(initRequest("GET", "https://example.com"))).isTrue();
    assertThat(predicate.test(initRequest("POST", "https://example.com"))).isFalse();
  }

  @Test
  void methodCorsPreFlight() {
    RequestPredicate predicate = RequestPredicates.method(HttpMethod.PUT);

    ServerRequest request = initRequest("OPTIONS", "https://example.com", servletRequest -> {
      servletRequest.addHeader("Origin", "https://example.com");
      servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT");
    });
    assertThat(predicate.test(request)).isTrue();

    request = initRequest("OPTIONS", "https://example.com", servletRequest -> {
      servletRequest.removeHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
      servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
    });
    assertThat(predicate.test(request)).isFalse();
  }

  @Test
  void methods() {
    RequestPredicate predicate = RequestPredicates.methods(HttpMethod.GET, HttpMethod.HEAD);
    assertThat(predicate.test(initRequest("GET", "https://example.com"))).isTrue();
    assertThat(predicate.test(initRequest("HEAD", "https://example.com"))).isTrue();
    assertThat(predicate.test(initRequest("POST", "https://example.com"))).isFalse();
  }

  @Test
  void allMethods() {
    RequestPredicate predicate = RequestPredicates.GET("/p*");
    assertThat(predicate.test(initRequest("GET", "/path"))).isTrue();

    predicate = RequestPredicates.HEAD("/p*");
    assertThat(predicate.test(initRequest("HEAD", "/path"))).isTrue();

    predicate = RequestPredicates.POST("/p*");
    assertThat(predicate.test(initRequest("POST", "/path"))).isTrue();

    predicate = RequestPredicates.PUT("/p*");
    assertThat(predicate.test(initRequest("PUT", "/path"))).isTrue();

    predicate = RequestPredicates.PATCH("/p*");
    assertThat(predicate.test(initRequest("PATCH", "/path"))).isTrue();

    predicate = RequestPredicates.DELETE("/p*");
    assertThat(predicate.test(initRequest("DELETE", "/path"))).isTrue();

    predicate = RequestPredicates.OPTIONS("/p*");
    assertThat(predicate.test(initRequest("OPTIONS", "/path"))).isTrue();
  }

  @Test
  void path() {
    RequestPredicate predicate = RequestPredicates.path("/p*");
    assertThat(predicate.test(initRequest("GET", "/path"))).isTrue();
    assertThat(predicate.test(initRequest("GET", "/foo"))).isFalse();
  }

  @Test
  void pathNoLeadingSlash() {
    RequestPredicate predicate = RequestPredicates.path("p*");
    assertThat(predicate.test(initRequest("GET", "/path"))).isTrue();
  }

  @Test
  void pathEncoded() {
    RequestPredicate predicate = RequestPredicates.path("/foo bar");
    assertThat(predicate.test(initRequest("GET", "/foo%20bar"))).isTrue();
    assertThat(predicate.test(initRequest("GET", ""))).isFalse();
  }

  @Test
  void pathPredicates() {
    PathPatternParser parser = new PathPatternParser();
    parser.setCaseSensitive(false);
    Function<String, RequestPredicate> pathPredicates = RequestPredicates.pathPredicates(parser);

    RequestPredicate predicate = pathPredicates.apply("/P*");
    assertThat(predicate.test(initRequest("GET", "/path"))).isTrue();
  }

  @Test
  public void pathWithContext() {
    RequestPredicate predicate = RequestPredicates.path("/p*");
    ServerRequest request = initRequest("GET", "/path", servletRequest -> { });
    assertThat(predicate.test(request)).isTrue();
  }

  @Test
  void headers() {
    String name = "MyHeader";
    String value = "MyValue";
    RequestPredicate predicate =
            RequestPredicates.headers(
                    headers -> headers.header(name).equals(Collections.singletonList(value)));
    ServerRequest request = initRequest("GET", "/path", req -> req.addHeader(name, value));
    assertThat(predicate.test(request)).isTrue();
    assertThat(predicate.test(initRequest("GET", ""))).isFalse();
  }

  @Test
  void headersCors() {
    RequestPredicate predicate = RequestPredicates.headers(headers -> false);
    ServerRequest request = initRequest("OPTIONS", "https://example.com", servletRequest -> {
      servletRequest.addHeader("Origin", "https://example.com");
      servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT");
    });
    assertThat(predicate.test(request)).isTrue();
  }

  @Test
  void contentType() {
    MediaType json = MediaType.APPLICATION_JSON;
    RequestPredicate predicate = RequestPredicates.contentType(json);
    ServerRequest request = initRequest("GET", "/path", req -> req.setContentType(json.toString()));
    assertThat(predicate.test(request)).isTrue();
    assertThat(predicate.test(initRequest("GET", ""))).isFalse();
  }

  @Test
  void accept() {
    MediaType json = MediaType.APPLICATION_JSON;
    RequestPredicate predicate = RequestPredicates.accept(json);
    ServerRequest request = initRequest("GET", "/path", req -> req.addHeader("Accept", json.toString()));
    assertThat(predicate.test(request)).isTrue();

    request = initRequest("GET", "", req -> req.addHeader("Accept", TEXT_XML_VALUE));
    assertThat(predicate.test(request)).isFalse();
  }

  @Test
  void pathExtension() {
    RequestPredicate predicate = RequestPredicates.pathExtension("txt");

    assertThat(predicate.test(initRequest("GET", "/file.txt"))).isTrue();
    assertThat(predicate.test(initRequest("GET", "/FILE.TXT"))).isTrue();

    predicate = RequestPredicates.pathExtension("bar");
    assertThat(predicate.test(initRequest("GET", "/FILE.TXT"))).isFalse();

    assertThat(predicate.test(initRequest("GET", "/file.foo"))).isFalse();
  }

  @Test
  void param() {
    RequestPredicate predicate = RequestPredicates.param("foo", "bar");
    ServerRequest request = initRequest("GET", "/path", req -> req.addParameter("foo", "bar"));
    assertThat(predicate.test(request)).isTrue();

    predicate = RequestPredicates.param("foo", s -> s.equals("bar"));
    assertThat(predicate.test(request)).isTrue();

    predicate = RequestPredicates.param("foo", "baz");
    assertThat(predicate.test(request)).isFalse();

    predicate = RequestPredicates.param("foo", s -> s.equals("baz"));
    assertThat(predicate.test(request)).isFalse();
  }

  @Test
  void version() {
    assertThat(RequestPredicates.version("1.1").test(serverRequest("1.1"))).isTrue();
    assertThat(RequestPredicates.version("1.1+").test(serverRequest("1.5"))).isTrue();
    assertThat(RequestPredicates.version("1.1").test(serverRequest("1.5"))).isFalse();
  }

  private static ServerRequest serverRequest(String version) {
    ApiVersionStrategy strategy = new DefaultApiVersionStrategy(
            List.of(exchange -> null), new SemanticApiVersionParser(), true, null, false, null);

    HttpMockRequestImpl mockRequest =
            PathPatternsTestUtils.initRequest("GET", null, "/path", true,
                    req -> req.setAttribute(API_VERSION_ATTRIBUTE, strategy.parseVersion(version)));

    return new DefaultServerRequest(new MockRequestContext(null, mockRequest,
            new MockHttpResponseImpl()), Collections.emptyList(), strategy);
  }

  private ServerRequest initRequest(String httpMethod, String requestUri) {
    return initRequest(httpMethod, requestUri, null);
  }

  private ServerRequest initRequest(
          String httpMethod, String requestUri, @Nullable Consumer<HttpMockRequestImpl> initializer) {
    HttpMockRequestImpl mockHttpServletRequest = PathPatternsTestUtils.initRequest(httpMethod, null, requestUri, true, initializer);
    return new DefaultServerRequest(
            new MockRequestContext(null, mockHttpServletRequest, new MockHttpResponseImpl()),
            Collections.emptyList());
  }

}
