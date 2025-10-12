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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.MediaType;
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

  @Test
  void header() {
    RequestPredicate predicate = RequestPredicates.headers(headers -> Objects.equals(headers.firstHeader("X-Custom"), "value"));
    ServerRequest request = initRequest("GET", "/path", req -> req.addHeader("X-Custom", "value"));
    assertThat(predicate.test(request)).isTrue();

    request = initRequest("GET", "/path", req -> req.addHeader("X-Custom", "other"));
    assertThat(predicate.test(request)).isFalse();

    request = initRequest("GET", "/path");
    assertThat(predicate.test(request)).isFalse();
  }

  @Test
  void contentTypeMultiple() {
    MediaType json = MediaType.APPLICATION_JSON;
    MediaType xml = MediaType.APPLICATION_XML;
    RequestPredicate predicate = RequestPredicates.contentType(json, xml);
    ServerRequest request = initRequest("POST", "/path", req -> req.setContentType(json.toString()));
    assertThat(predicate.test(request)).isTrue();

    request = initRequest("POST", "/path", req -> req.setContentType(xml.toString()));
    assertThat(predicate.test(request)).isTrue();

    request = initRequest("POST", "/path", req -> req.setContentType(MediaType.TEXT_PLAIN.toString()));
    assertThat(predicate.test(request)).isFalse();
  }

  @Test
  void acceptMultiple() {
    MediaType json = MediaType.APPLICATION_JSON;
    MediaType xml = MediaType.APPLICATION_XML;
    RequestPredicate predicate = RequestPredicates.accept(json, xml);
    ServerRequest request = initRequest("GET", "/path", req -> req.addHeader("Accept", json.toString()));
    assertThat(predicate.test(request)).isTrue();

    request = initRequest("GET", "/path", req -> req.addHeader("Accept", xml.toString()));
    assertThat(predicate.test(request)).isTrue();

    request = initRequest("GET", "/path", req -> req.addHeader("Accept", MediaType.TEXT_PLAIN.toString()));
    assertThat(predicate.test(request)).isFalse();
  }

  @Test
  void pathExtensionPredicate() {
    Predicate<String> extensionPredicate = ext -> "txt".equalsIgnoreCase(ext) || "log".equalsIgnoreCase(ext);
    RequestPredicate predicate = RequestPredicates.pathExtension(extensionPredicate);

    assertThat(predicate.test(initRequest("GET", "/file.txt"))).isTrue();
    assertThat(predicate.test(initRequest("GET", "/file.log"))).isTrue();
    assertThat(predicate.test(initRequest("GET", "/file.xml"))).isFalse();
  }

  @Test
  void andPredicate() {
    RequestPredicate predicate1 = RequestPredicates.method(HttpMethod.GET);
    RequestPredicate predicate2 = RequestPredicates.path("/path");
    RequestPredicate andPredicate = predicate1.and(predicate2);

    assertThat(andPredicate.test(initRequest("GET", "/path"))).isTrue();
    assertThat(andPredicate.test(initRequest("POST", "/path"))).isFalse();
    assertThat(andPredicate.test(initRequest("GET", "/other"))).isFalse();
  }

  @Test
  void orPredicate() {
    RequestPredicate predicate1 = RequestPredicates.method(HttpMethod.GET);
    RequestPredicate predicate2 = RequestPredicates.method(HttpMethod.POST);
    RequestPredicate orPredicate = predicate1.or(predicate2);

    assertThat(orPredicate.test(initRequest("GET", "/path"))).isTrue();
    assertThat(orPredicate.test(initRequest("POST", "/path"))).isTrue();
    assertThat(orPredicate.test(initRequest("PUT", "/path"))).isFalse();
  }

  @Test
  void negatePredicate() {
    RequestPredicate predicate = RequestPredicates.method(HttpMethod.GET);
    RequestPredicate negatePredicate = predicate.negate();

    assertThat(negatePredicate.test(initRequest("GET", "/path"))).isFalse();
    assertThat(negatePredicate.test(initRequest("POST", "/path"))).isTrue();
  }

  @Test
  void complexPredicateCombination() {
    RequestPredicate getPredicate = RequestPredicates.method(HttpMethod.GET);
    RequestPredicate postPredicate = RequestPredicates.method(HttpMethod.POST);
    RequestPredicate pathPredicate = RequestPredicates.path("/api/*");
    RequestPredicate contentTypePredicate = RequestPredicates.contentType(MediaType.APPLICATION_JSON);

    RequestPredicate complexPredicate = getPredicate.and(pathPredicate)
            .or(postPredicate.and(pathPredicate).and(contentTypePredicate));

    assertThat(complexPredicate.test(initRequest("GET", "/api/users"))).isTrue();
//    assertThat(complexPredicate.test(initRequest("POST", "/api/users"))).isTrue();
    assertThat(complexPredicate.test(initRequest("POST", "/api/users", req -> req.setContentType(MediaType.APPLICATION_JSON_VALUE)))).isTrue();
    assertThat(complexPredicate.test(initRequest("PUT", "/api/users"))).isFalse();
  }

  @Test
  void pathNest() {
    RequestPredicate predicate = RequestPredicates.path("/api/**");
    ServerRequest request = initRequest("GET", "/api/users/123");
    Optional<ServerRequest> nestedRequest = predicate.nest(request);

    assertThat(nestedRequest).isPresent();
    assertThat(nestedRequest.get().path()).isEqualTo("/api/users/123");
  }

  @Test
  void pathNestFailure() {
    RequestPredicate predicate = RequestPredicates.path("/admin/**");
    ServerRequest request = initRequest("GET", "/api/users/123");
    Optional<ServerRequest> nestedRequest = predicate.nest(request);

    assertThat(nestedRequest).isEmpty();
  }

  @Test
  void paramWithPredicate() {
    RequestPredicate predicate = RequestPredicates.param("foo", s -> s.startsWith("bar"));
    ServerRequest request = initRequest("GET", "/path", req -> req.addParameter("foo", "bar123"));
    assertThat(predicate.test(request)).isTrue();

    request = initRequest("GET", "/path", req -> req.addParameter("foo", "baz"));
    assertThat(predicate.test(request)).isFalse();
  }

  @Test
  void methodsSingle() {
    RequestPredicate predicate = RequestPredicates.methods(HttpMethod.GET);
    assertThat(predicate.test(initRequest("GET", "/path"))).isTrue();
    assertThat(predicate.test(initRequest("POST", "/path"))).isFalse();
  }

  @Test
  void pathWithVariables() {
    RequestPredicate predicate = RequestPredicates.path("/users/{id}");
    ServerRequest request = initRequest("GET", "/users/123");
    assertThat(predicate.test(request)).isTrue();

    Optional<ServerRequest> nestedRequest = predicate
            .nest(initRequest("GET", "/users/123"));
    assertThat(nestedRequest).isPresent();
    assertThat(nestedRequest.get().pathVariables()).containsEntry("id", "123");
  }

  @Test
  void pathNoMatch() {
    RequestPredicate predicate = RequestPredicates.path("/api/**");
    ServerRequest request = initRequest("GET", "/admin/users");
    assertThat(predicate.test(request)).isFalse();
  }

  @Test
  void acceptAll() {
    RequestPredicate predicate = RequestPredicates.accept(MediaType.ALL);
    ServerRequest request = initRequest("GET", "/path", req -> req.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE));
    assertThat(predicate.test(request)).isTrue();
  }

  @Test
  void contentTypeAll() {
    RequestPredicate predicate = RequestPredicates.contentType(MediaType.ALL);
    ServerRequest request = initRequest("POST", "/path", req -> req.setContentType(MediaType.APPLICATION_XML_VALUE));
    assertThat(predicate.test(request)).isTrue();
  }

  @Test
  void negateComplexPredicate() {
    RequestPredicate methodPredicate = RequestPredicates.method(HttpMethod.GET);
    RequestPredicate pathPredicate = RequestPredicates.path("/api/**");
    RequestPredicate complexPredicate = methodPredicate.and(pathPredicate).negate();

    assertThat(complexPredicate.test(initRequest("GET", "/api/users"))).isFalse();
    assertThat(complexPredicate.test(initRequest("POST", "/api/users"))).isTrue();
    assertThat(complexPredicate.test(initRequest("GET", "/admin/users"))).isTrue();
  }

  @Test
  void andWithThreePredicates() {
    RequestPredicate methodPredicate = RequestPredicates.method(HttpMethod.POST);
    RequestPredicate pathPredicate = RequestPredicates.path("/api/**");
    RequestPredicate contentTypePredicate = RequestPredicates.contentType(MediaType.APPLICATION_JSON);

    RequestPredicate combined = methodPredicate.and(pathPredicate).and(contentTypePredicate);

    ServerRequest request = initRequest("POST", "/api/users", req -> req.setContentType(MediaType.APPLICATION_JSON_VALUE));
    assertThat(combined.test(request)).isTrue();

    request = initRequest("POST", "/api/users", req -> req.setContentType(MediaType.TEXT_PLAIN_VALUE));
    assertThat(combined.test(request)).isFalse();
  }

  @Test
  void orWithThreePredicates() {
    RequestPredicate getPredicate = RequestPredicates.method(HttpMethod.GET);
    RequestPredicate postPredicate = RequestPredicates.method(HttpMethod.POST);
    RequestPredicate putPredicate = RequestPredicates.method(HttpMethod.PUT);

    RequestPredicate combined = getPredicate.or(postPredicate).or(putPredicate);

    assertThat(combined.test(initRequest("GET", "/path"))).isTrue();
    assertThat(combined.test(initRequest("POST", "/path"))).isTrue();
    assertThat(combined.test(initRequest("PUT", "/path"))).isTrue();
    assertThat(combined.test(initRequest("DELETE", "/path"))).isFalse();
  }

  @Test
  void pathExtensionWithNullExtension() {
    RequestPredicate predicate = RequestPredicates.pathExtension("");
    ServerRequest request = initRequest("GET", "/file.");
    assertThat(predicate.test(request)).isTrue();
  }

  @Test
  void headersWithMultipleConditions() {
    RequestPredicate predicate = RequestPredicates.headers(headers ->
            "value1".equals(headers.firstHeader("Header1")) &&
                    "value2".equals(headers.firstHeader("Header2")));

    ServerRequest request = initRequest("GET", "/path", req -> {
      req.addHeader("Header1", "value1");
      req.addHeader("Header2", "value2");
    });
    assertThat(predicate.test(request)).isTrue();

    request = initRequest("GET", "/path", req -> {
      req.addHeader("Header1", "value1");
      req.addHeader("Header2", "wrong");
    });
    assertThat(predicate.test(request)).isFalse();
  }

  @Test
  void contentTypeWithWildcard() {
    RequestPredicate predicate = RequestPredicates.contentType(MediaType.parseMediaType("text/*"));
    ServerRequest request = initRequest("POST", "/path", req -> req.setContentType(MediaType.TEXT_PLAIN_VALUE));
    assertThat(predicate.test(request)).isTrue();

    request = initRequest("POST", "/path", req -> req.setContentType(MediaType.APPLICATION_JSON_VALUE));
    assertThat(predicate.test(request)).isFalse();
  }

  @Test
  void acceptWithWildcard() {
    RequestPredicate predicate = RequestPredicates.accept(MediaType.parseMediaType("application/*"));
    ServerRequest request = initRequest("GET", "/path", req -> req.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE));
    assertThat(predicate.test(request)).isTrue();

    request = initRequest("GET", "/path", req -> req.addHeader("Accept", MediaType.TEXT_PLAIN_VALUE));
    assertThat(predicate.test(request)).isFalse();
  }

  @Test
  void paramExistsWithEmptyValue() {
    RequestPredicate predicate = RequestPredicates.param("foo");
    ServerRequest request = initRequest("GET", "/path", req -> req.addParameter("foo", ""));
    assertThat(predicate.test(request)).isTrue();
  }

  @Test
  void paramNotExists() {
    RequestPredicate predicate = RequestPredicates.param("foo");
    ServerRequest request = initRequest("GET", "/path");
    assertThat(predicate.test(request)).isFalse();
  }

  @Test
  void paramWithCustomPredicate() {
    RequestPredicate predicate = RequestPredicates.param("number", s -> s.matches("\\d+"));
    ServerRequest request = initRequest("GET", "/path", req -> req.addParameter("number", "123"));
    assertThat(predicate.test(request)).isTrue();

    request = initRequest("GET", "/path", req -> req.addParameter("number", "abc"));
    assertThat(predicate.test(request)).isFalse();
  }

  @Test
  void allMethodsPredicates() {
    assertThat(RequestPredicates.GET("/test").test(initRequest("GET", "/test"))).isTrue();
    assertThat(RequestPredicates.HEAD("/test").test(initRequest("HEAD", "/test"))).isTrue();
    assertThat(RequestPredicates.POST("/test").test(initRequest("POST", "/test"))).isTrue();
    assertThat(RequestPredicates.PUT("/test").test(initRequest("PUT", "/test"))).isTrue();
    assertThat(RequestPredicates.PATCH("/test").test(initRequest("PATCH", "/test"))).isTrue();
    assertThat(RequestPredicates.DELETE("/test").test(initRequest("DELETE", "/test"))).isTrue();
    assertThat(RequestPredicates.OPTIONS("/test").test(initRequest("OPTIONS", "/test"))).isTrue();
  }

  @Test
  void allMethodsPredicatesFailure() {
    assertThat(RequestPredicates.GET("/test").test(initRequest("POST", "/test"))).isFalse();
    assertThat(RequestPredicates.HEAD("/test").test(initRequest("GET", "/test"))).isFalse();
    assertThat(RequestPredicates.POST("/test").test(initRequest("PUT", "/test"))).isFalse();
    assertThat(RequestPredicates.PUT("/test").test(initRequest("PATCH", "/test"))).isFalse();
    assertThat(RequestPredicates.PATCH("/test").test(initRequest("DELETE", "/test"))).isFalse();
    assertThat(RequestPredicates.DELETE("/test").test(initRequest("OPTIONS", "/test"))).isFalse();
    assertThat(RequestPredicates.OPTIONS("/test").test(initRequest("GET", "/test"))).isFalse();
  }

  @Test
  void pathWithMultipleVariables() {
    RequestPredicate predicate = RequestPredicates.path("/users/{userId}/posts/{postId}");
    ServerRequest request = initRequest("GET", "/users/123/posts/456");
    assertThat(predicate.test(request)).isTrue();

    Optional<ServerRequest> nestedRequest = predicate.nest(initRequest("GET", "/users/123/posts/456"));
    assertThat(nestedRequest).isPresent();
    assertThat(nestedRequest.get().pathVariables()).containsEntry("userId", "123");
    assertThat(nestedRequest.get().pathVariables()).containsEntry("postId", "456");
  }

  @Test
  void pathWithEncodedCharacters() {
    RequestPredicate predicate = RequestPredicates.path("/search/{query}");
    ServerRequest request = initRequest("GET", "/search/hello%20world");
    assertThat(predicate.test(request)).isTrue();

    Optional<ServerRequest> nestedRequest = predicate.nest(initRequest("GET", "/search/hello%20world"));
    assertThat(nestedRequest).isPresent();
    assertThat(nestedRequest.get().pathVariables()).containsEntry("query", "hello world");
  }

  @Test
  void contentTypeWithQualityParameter() {
    RequestPredicate predicate = RequestPredicates.contentType(MediaType.APPLICATION_JSON);
    ServerRequest request = initRequest("POST", "/path", req -> req.setContentType("application/json;q=0.9"));
    assertThat(predicate.test(request)).isTrue();
  }

  @Test
  void acceptWithQualityParameter() {
    RequestPredicate predicate = RequestPredicates.accept(MediaType.APPLICATION_JSON);
    ServerRequest request = initRequest("GET", "/path", req -> req.addHeader("Accept", "application/json;q=0.9"));
    assertThat(predicate.test(request)).isTrue();
  }

  @Test
  void pathExtensionWithMultipleDots() {
    RequestPredicate predicate = RequestPredicates.pathExtension("gz");
    ServerRequest request = initRequest("GET", "/file.tar.gz");
    assertThat(predicate.test(request)).isTrue();
  }

  @Test
  void pathExtensionCaseSensitivity() {
    RequestPredicate predicate = RequestPredicates.pathExtension("TXT");
    ServerRequest request = initRequest("GET", "/file.txt");
    assertThat(predicate.test(request)).isTrue();
  }

  @Test
  void headersWithComplexCondition() {
    RequestPredicate predicate = RequestPredicates.headers(headers ->
            headers.header("X-API-Key").size() == 1 &&
                    headers.firstHeader("X-API-Key") != null &&
                    headers.firstHeader("X-API-Key").startsWith("key-"));

    ServerRequest request = initRequest("GET", "/path", req -> req.addHeader("X-API-Key", "key-12345"));
    assertThat(predicate.test(request)).isTrue();

    request = initRequest("GET", "/path", req -> req.addHeader("X-API-Key", "invalid-key"));
    assertThat(predicate.test(request)).isFalse();
  }

  @Test
  void contentTypeMultipleWithWildcard() {
    RequestPredicate predicate = RequestPredicates.contentType(
            MediaType.APPLICATION_JSON,
            MediaType.parseMediaType("text/*"));

    ServerRequest request = initRequest("POST", "/path", req -> req.setContentType(MediaType.APPLICATION_JSON_VALUE));
    assertThat(predicate.test(request)).isTrue();

    request = initRequest("POST", "/path", req -> req.setContentType(MediaType.TEXT_PLAIN_VALUE));
    assertThat(predicate.test(request)).isTrue();

    request = initRequest("POST", "/path", req -> req.setContentType(MediaType.APPLICATION_XML_VALUE));
    assertThat(predicate.test(request)).isFalse();
  }

  @Test
  void acceptMultipleWithQualityValues() {
    RequestPredicate predicate = RequestPredicates.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_XML);
    ServerRequest request = initRequest("GET", "/path", req -> req.addHeader("Accept", "application/json;q=0.9,text/xml;q=0.8"));
    assertThat(predicate.test(request)).isTrue();
  }

  @Test
  void andPredicateWithThreeOperands() {
    RequestPredicate methodPredicate = RequestPredicates.method(HttpMethod.POST);
    RequestPredicate pathPredicate = RequestPredicates.path("/api/data");
    RequestPredicate contentTypePredicate = RequestPredicates.contentType(MediaType.APPLICATION_JSON);

    RequestPredicate combined = methodPredicate.and(pathPredicate).and(contentTypePredicate);

    ServerRequest request = initRequest("POST", "/api/data", req -> req.setContentType(MediaType.APPLICATION_JSON_VALUE));
    assertThat(combined.test(request)).isTrue();

    request = initRequest("GET", "/api/data", req -> req.setContentType(MediaType.APPLICATION_JSON_VALUE));
    assertThat(combined.test(request)).isFalse();

    request = initRequest("POST", "/api/users", req -> req.setContentType(MediaType.APPLICATION_JSON_VALUE));
    assertThat(combined.test(request)).isFalse();

    request = initRequest("POST", "/api/data", req -> req.setContentType(MediaType.TEXT_PLAIN_VALUE));
    assertThat(combined.test(request)).isFalse();
  }

  @Test
  void orPredicateWithThreeOperands() {
    RequestPredicate jsonPredicate = RequestPredicates.contentType(MediaType.APPLICATION_JSON);
    RequestPredicate xmlPredicate = RequestPredicates.contentType(MediaType.APPLICATION_XML);
    RequestPredicate textPredicate = RequestPredicates.contentType(MediaType.TEXT_PLAIN);

    RequestPredicate combined = jsonPredicate.or(xmlPredicate).or(textPredicate);

    ServerRequest request = initRequest("POST", "/path", req -> req.setContentType(MediaType.APPLICATION_JSON_VALUE));
    assertThat(combined.test(request)).isTrue();

    request = initRequest("POST", "/path", req -> req.setContentType(MediaType.APPLICATION_XML_VALUE));
    assertThat(combined.test(request)).isTrue();

    request = initRequest("POST", "/path", req -> req.setContentType(MediaType.TEXT_PLAIN_VALUE));
    assertThat(combined.test(request)).isTrue();

    request = initRequest("POST", "/path", req -> req.setContentType(MediaType.IMAGE_PNG_VALUE));
    assertThat(combined.test(request)).isFalse();
  }

  @Test
  void complexNestedPredicates() {
    RequestPredicate getApi = RequestPredicates.method(HttpMethod.GET).and(RequestPredicates.path("/api/**"));
    RequestPredicate postJson = RequestPredicates.method(HttpMethod.POST).and(RequestPredicates.contentType(MediaType.APPLICATION_JSON));

    RequestPredicate complex = getApi.or(postJson);

    assertThat(complex.test(initRequest("GET", "/api/users"))).isTrue();
    assertThat(complex.test(initRequest("POST", "/data", req -> req.setContentType(MediaType.APPLICATION_JSON_VALUE)))).isTrue();
    assertThat(complex.test(initRequest("PUT", "/api/users"))).isFalse();
    assertThat(complex.test(initRequest("POST", "/data", req -> req.setContentType(MediaType.TEXT_PLAIN_VALUE)))).isFalse();
  }

  @Test
  void negateOfComplexPredicate() {
    RequestPredicate apiPredicate = RequestPredicates.path("/api/**");
    RequestPredicate negated = apiPredicate.negate();

    assertThat(negated.test(initRequest("GET", "/api/users"))).isFalse();
    assertThat(negated.test(initRequest("GET", "/public/info"))).isTrue();
  }

  @Test
  void pathNestWithVariables() {
    RequestPredicate predicate = RequestPredicates.path("/api/{version}/users/**");
    ServerRequest request = initRequest("GET", "/api/v1/users/123/details");

    Optional<ServerRequest> nestedRequest = predicate.nest(request);
    assertThat(nestedRequest).isPresent();
    assertThat(nestedRequest.get().pathVariables()).containsEntry("version", "v1");
  }

  @Test
  void pathNestExactMatch() {
    RequestPredicate predicate = RequestPredicates.path("/api/users");
    ServerRequest request = initRequest("GET", "/api/users");

    Optional<ServerRequest> nestedRequest = predicate.nest(request);
    assertThat(nestedRequest).isPresent();
  }

  @Test
  void versionPredicateWithBaseline() {
    ApiVersionStrategy strategy = new DefaultApiVersionStrategy(
            List.of(exchange -> null), new SemanticApiVersionParser(), true,
            null, false, null, null);

    assertThat(RequestPredicates.version("2.0+").test(serverRequest("2.5"))).isTrue();
    assertThat(RequestPredicates.version("2.0+").test(serverRequest("2.0"))).isTrue();
    assertThat(RequestPredicates.version("2.0+").test(serverRequest("1.9"))).isFalse();
  }

  private static ServerRequest serverRequest(String version) {
    ApiVersionStrategy strategy = new DefaultApiVersionStrategy(
            List.of(exchange -> null), new SemanticApiVersionParser(), true,
            null, false, null, null);

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
