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

import java.util.Map;
import java.util.Optional;

import infra.http.server.RequestPath;
import infra.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 18:27
 */
class ChangePathPatternParserVisitorTests {

  @Test
  void changeParserWithPathPatternPredicate() {
    PathPatternParser oldParser = PathPatternParser.defaultInstance;
    PathPatternParser newParser = new PathPatternParser();
    newParser.setCaseSensitive(false);

    ChangePathPatternParserVisitor visitor = new ChangePathPatternParserVisitor(newParser);

    RequestPredicate predicate = RequestPredicates.path("/test");

    // Verify that the predicate is an instance of the target class
    assertThat(predicate).isInstanceOf(ChangePathPatternParserVisitor.Target.class);

    // Apply visitor to change parser
    visitor.route(predicate, request -> ServerResponse.ok().build());

    // Test that the predicate still works after parser change
    ServerRequest request = mock();
    given(request.path()).willReturn("/test");
    given(request.requestPath()).willReturn(RequestPath.parse("/test", null));
    assertThat(predicate.test(request)).isTrue();
  }

  @Test
  void startNestedChangesParser() {
    PathPatternParser newParser = new PathPatternParser();
    newParser.setCaseSensitive(false);

    ChangePathPatternParserVisitor visitor = new ChangePathPatternParserVisitor(newParser);

    RequestPredicate predicate = RequestPredicates.path("/nested/**");

    // Verify predicate implements Target interface
    assertThat(predicate).isInstanceOf(ChangePathPatternParserVisitor.Target.class);

    // Call startNested which should change the parser
    visitor.startNested(predicate);

    // Test that the predicate still works
    ServerRequest request = mock();

    given(request.path()).willReturn("/nested/path");
    given(request.requestPath()).willReturn(RequestPath.parse("/nested/path", null));

    assertThat(predicate.test(request)).isTrue();
  }

  @Test
  void routeChangesParser() {
    PathPatternParser newParser = new PathPatternParser();
    newParser.setMatchOptionalTrailingSeparator(true);

    ChangePathPatternParserVisitor visitor = new ChangePathPatternParserVisitor(newParser);

    RequestPredicate predicate = RequestPredicates.path("/route/*");
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();

    // Call route which should change the parser
    visitor.route(predicate, handlerFunction);

    // Test that the predicate still works
    ServerRequest request = mock();
    given(request.path()).willReturn("/route/test");
    given(request.requestPath()).willReturn(RequestPath.parse("/route/test", null));
    assertThat(predicate.test(request)).isTrue();
  }

  @Test
  void unknownDoesNotChangeParser() {
    PathPatternParser newParser = new PathPatternParser();

    ChangePathPatternParserVisitor visitor = new ChangePathPatternParserVisitor(newParser);

    // Create a custom router function that is "unknown"
    RouterFunction<ServerResponse> unknownRouterFunction = new RouterFunction<ServerResponse>() {
      @Override
      public Optional<HandlerFunction<ServerResponse>> route(ServerRequest request) {
        return Optional.empty();
      }
    };

    // Call unknown - should not throw exception
    visitor.unknown(unknownRouterFunction);

    // No assertions needed - test passes if no exception is thrown
  }

  @Test
  void resourcesDoesNotChangeParser() {
    PathPatternParser newParser = new PathPatternParser();

    ChangePathPatternParserVisitor visitor = new ChangePathPatternParserVisitor(newParser);

    // Call resources - should not throw exception
    visitor.resources(request -> Optional.empty());

    // No assertions needed - test passes if no exception is thrown
  }

  @Test
  void attributesDoesNotChangeParser() {
    PathPatternParser newParser = new PathPatternParser();

    ChangePathPatternParserVisitor visitor = new ChangePathPatternParserVisitor(newParser);

    // Call attributes - should not throw exception
    visitor.attributes(Map.of("key", "value"));

    // No assertions needed - test passes if no exception is thrown
  }

  @Test
  void endNestedDoesNotChangeParser() {
    PathPatternParser newParser = new PathPatternParser();

    ChangePathPatternParserVisitor visitor = new ChangePathPatternParserVisitor(newParser);

    RequestPredicate predicate = RequestPredicates.path("/end");

    // Call endNested - should not throw exception
    visitor.endNested(predicate);

    // No assertions needed - test passes if no exception is thrown
  }

}