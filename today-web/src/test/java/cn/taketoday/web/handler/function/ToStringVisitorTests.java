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

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;

import static cn.taketoday.web.handler.function.RequestPredicates.GET;
import static cn.taketoday.web.handler.function.RequestPredicates.accept;
import static cn.taketoday.web.handler.function.RequestPredicates.contentType;
import static cn.taketoday.web.handler.function.RequestPredicates.method;
import static cn.taketoday.web.handler.function.RequestPredicates.methods;
import static cn.taketoday.web.handler.function.RequestPredicates.param;
import static cn.taketoday.web.handler.function.RequestPredicates.path;
import static cn.taketoday.web.handler.function.RequestPredicates.pathExtension;
import static cn.taketoday.web.handler.function.RouterFunctions.route;
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
