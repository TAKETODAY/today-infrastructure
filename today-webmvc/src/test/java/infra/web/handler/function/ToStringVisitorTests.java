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

import infra.http.HttpMethod;
import infra.http.MediaType;

import static infra.web.handler.function.RequestPredicates.GET;
import static infra.web.handler.function.RequestPredicates.accept;
import static infra.web.handler.function.RequestPredicates.contentType;
import static infra.web.handler.function.RequestPredicates.method;
import static infra.web.handler.function.RequestPredicates.methods;
import static infra.web.handler.function.RequestPredicates.param;
import static infra.web.handler.function.RequestPredicates.path;
import static infra.web.handler.function.RequestPredicates.pathExtension;
import static infra.web.handler.function.RouterFunctions.route;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class ToStringVisitorTests {

  @Test
  public void nested() {
    HandlerFunction<ServerResponse> handler = new SimpleHandlerFunction();
    RouterFunction<ServerResponse> routerFunction = route()
            .path("/foo", builder ->
                    builder.path("/bar", () -> route()
                            .GET("/baz", handler)
                            .build())
            )
            .build();

    ToStringVisitor visitor = new ToStringVisitor();
    routerFunction.accept(visitor);
    String result = visitor.toString();

    String expected = """
            /foo => {
             /bar => {
              (GET && /baz) ->\s
             }
            }""";
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void predicates() {
    testPredicate(methods(HttpMethod.GET), "GET");
//    testPredicate(methods(HttpMethod.GET, HttpMethod.POST), "[GET, POST]");

    testPredicate(path("/foo"), "/foo");

    testPredicate(pathExtension("foo"), "*.foo");

    testPredicate(contentType(MediaType.APPLICATION_JSON), "Content-Type: application/json");

    ToStringVisitor visitor = new ToStringVisitor();
    contentType(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN).accept(visitor);
    assertThat(visitor.toString()).matches("Content-Type: \\[.+, .+\\]").contains("application/json", "text/plain");

    testPredicate(accept(MediaType.APPLICATION_JSON), "Accept: application/json");

    testPredicate(param("foo", "bar"), "?foo == bar");

    testPredicate(method(HttpMethod.GET).and(path("/foo")), "(GET && /foo)");

    testPredicate(method(HttpMethod.GET).or(path("/foo")), "(GET || /foo)");

    testPredicate(method(HttpMethod.GET).negate(), "!(GET)");

    testPredicate(GET("/foo")
                    .or(contentType(MediaType.TEXT_PLAIN))
                    .and(accept(MediaType.APPLICATION_JSON).negate()),
            "(((GET && /foo) || Content-Type: text/plain) && !(Accept: application/json))");
  }

  @Test
  public void singleRoute() {
    HandlerFunction<ServerResponse> handler = new SimpleHandlerFunction();
    RouterFunction<ServerResponse> routerFunction = route()
            .GET("/foo", handler)
            .build();

    ToStringVisitor visitor = new ToStringVisitor();
    routerFunction.accept(visitor);
    String result = visitor.toString();

    String expected = "(GET && /foo) -> ";
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void multipleRoutes() {
    HandlerFunction<ServerResponse> handler = new SimpleHandlerFunction();
    RouterFunction<ServerResponse> routerFunction = route()
            .GET("/foo", handler)
            .POST("/bar", handler)
            .build();

    ToStringVisitor visitor = new ToStringVisitor();
    routerFunction.accept(visitor);
    String result = visitor.toString();

    String expected = """
            (GET && /foo) ->\s
            (POST && /bar) ->\s""";
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void nestedWithPath() {
    HandlerFunction<ServerResponse> handler = new SimpleHandlerFunction();
    RouterFunction<ServerResponse> routerFunction = route()
            .path("/api", builder ->
                    builder.GET("/users", handler)
            )
            .build();

    ToStringVisitor visitor = new ToStringVisitor();
    routerFunction.accept(visitor);
    String result = visitor.toString();

    String expected = """
            /api => {
             (GET && /users) ->\s
            }""";
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void complexNested() {
    HandlerFunction<ServerResponse> handler = new SimpleHandlerFunction();
    RouterFunction<ServerResponse> routerFunction = route()
            .path("/api", builder ->
                    builder.path("/v1", b ->
                            b.GET("/users", handler)
                                    .POST("/users", handler)
                    )
            )
            .build();

    ToStringVisitor visitor = new ToStringVisitor();
    routerFunction.accept(visitor);
    String result = visitor.toString();

    String expected = """
            /api => {
             /v1 => {
              (GET && /users) ->\s
              (POST && /users) ->\s
             }
            }""";
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void versionPredicate() {
    ToStringVisitor visitor = new ToStringVisitor();
    RequestPredicates.version("1.0").accept(visitor);
    String result = visitor.toString();

    assertThat(result).isEqualTo("version: 1.0");
  }

  @Test
  public void paramPredicate() {
    ToStringVisitor visitor = new ToStringVisitor();
    RequestPredicates.param("page", "1").accept(visitor);
    String result = visitor.toString();

    assertThat(result).isEqualTo("?page == 1");
  }

  @Test
  public void pathExtensionPredicate() {
    ToStringVisitor visitor = new ToStringVisitor();
    RequestPredicates.pathExtension("json").accept(visitor);
    String result = visitor.toString();

    assertThat(result).isEqualTo("*.json");
  }

  @Test
  public void multipleMethodsPredicate() {
    ToStringVisitor visitor = new ToStringVisitor();
    RequestPredicates.methods(HttpMethod.GET, HttpMethod.POST).accept(visitor);
    String result = visitor.toString();

    assertThat(result).isEqualTo("[GET, POST]");
  }

  @Test
  public void complexAndCombination() {
    RequestPredicate predicate = RequestPredicates.method(HttpMethod.POST)
            .and(RequestPredicates.path("/api/data"))
            .and(RequestPredicates.contentType(MediaType.APPLICATION_JSON));

    ToStringVisitor visitor = new ToStringVisitor();
    predicate.accept(visitor);
    String result = visitor.toString();

    assertThat(result).isEqualTo("((POST && /api/data) && Content-Type: application/json)");
  }

  @Test
  public void complexOrCombination() {
    RequestPredicate predicate = RequestPredicates.method(HttpMethod.GET)
            .or(RequestPredicates.method(HttpMethod.POST));

    ToStringVisitor visitor = new ToStringVisitor();
    predicate.accept(visitor);
    String result = visitor.toString();

    assertThat(result).isEqualTo("(GET || POST)");
  }

  @Test
  public void nestedLogicalOperations() {
    RequestPredicate predicate = RequestPredicates.method(HttpMethod.GET)
            .and(RequestPredicates.path("/api"))
            .or(RequestPredicates.method(HttpMethod.POST)
                    .and(RequestPredicates.path("/admin")));

    ToStringVisitor visitor = new ToStringVisitor();
    predicate.accept(visitor);
    String result = visitor.toString();

    assertThat(result).isEqualTo("((GET && /api) || (POST && /admin))");
  }

  @Test
  public void negateComplexPredicate() {
    RequestPredicate predicate = RequestPredicates.method(HttpMethod.GET)
            .and(RequestPredicates.path("/api"))
            .negate();

    ToStringVisitor visitor = new ToStringVisitor();
    predicate.accept(visitor);
    String result = visitor.toString();

    assertThat(result).isEqualTo("!((GET && /api))");
  }

  @Test
  public void mixedPredicatesWithNesting() {
    HandlerFunction<ServerResponse> handler = new SimpleHandlerFunction();
    RouterFunction<ServerResponse> routerFunction = route()
            .path("/api", builder ->
                    builder.GET("/users", handler)
                            .POST("/users", handler)
                            .path("/v2", b ->
                                    b.GET("/profiles", handler)
                            )
            )
            .build();

    ToStringVisitor visitor = new ToStringVisitor();
    routerFunction.accept(visitor);
    String result = visitor.toString();

    String expected = """
            /api => {
             (GET && /users) ->\s
             (POST && /users) ->\s
             /v2 => {
              (GET && /profiles) ->\s
             }
            }""";
    assertThat(result).isEqualTo(expected);
  }

  private void testPredicate(RequestPredicate predicate, String expected) {
    ToStringVisitor visitor = new ToStringVisitor();
    predicate.accept(visitor);
    String result = visitor.toString();

    assertThat(result).isEqualTo(expected);
  }

  private static class SimpleHandlerFunction implements HandlerFunction<ServerResponse> {

    @Override
    public ServerResponse handle(ServerRequest request) {
      return ServerResponse.ok().build();
    }

    @Override
    public String toString() {
      return "";
    }
  }

}
