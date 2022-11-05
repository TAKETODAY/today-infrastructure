/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.handler.function;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.util.pattern.PathPatternParser;
import cn.taketoday.web.view.PathPatternsTestUtils;

import static cn.taketoday.http.MediaType.TEXT_XML_VALUE;
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
  void contextPath() {
    RequestPredicate predicate = RequestPredicates.path("/bar");

    MockHttpServletRequest initRequest = PathPatternsTestUtils.initRequest("GET", "/foo", "/bar", true);
    ServerRequest request = new DefaultServerRequest(
            new ServletRequestContext(null, initRequest, new MockHttpServletResponse()),
            Collections.emptyList());

    assertThat(predicate.test(request)).isTrue();

    request = initRequest("GET", "/foo");
    assertThat(predicate.test(request)).isFalse();
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
    ServerRequest request = initRequest("GET", "/context/path",
            servletRequest -> servletRequest.setContextPath("/context"));
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

  private ServerRequest initRequest(String httpMethod, String requestUri) {
    return initRequest(httpMethod, requestUri, null);
  }

  private ServerRequest initRequest(
          String httpMethod, String requestUri, @Nullable Consumer<MockHttpServletRequest> initializer) {
    MockHttpServletRequest mockHttpServletRequest = PathPatternsTestUtils.initRequest(httpMethod, null, requestUri, true, initializer);
    return new DefaultServerRequest(
            new ServletRequestContext(null, mockHttpServletRequest, new MockHttpServletResponse()),
            Collections.emptyList());
  }

}
