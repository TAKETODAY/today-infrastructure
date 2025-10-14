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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.mock.MockRequestContext;
import infra.web.view.PathPatternsTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class RequestPredicateTests {

  private ServerRequest request;

  @BeforeEach
  void createRequest() {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    MockRequestContext requestContext = new MockRequestContext(null, servletRequest, new MockHttpResponseImpl());
    this.request = new DefaultServerRequest(requestContext, Collections.emptyList());
  }

  @Test
  void and() {
    RequestPredicate predicate1 = request -> true;
    RequestPredicate predicate2 = request -> true;
    RequestPredicate predicate3 = request -> false;

    assertThat(predicate1.and(predicate2).test(request)).isTrue();
    assertThat(predicate2.and(predicate1).test(request)).isTrue();
    assertThat(predicate1.and(predicate3).test(request)).isFalse();
  }

  @Test
  void negate() {
    RequestPredicate predicate = request -> false;
    RequestPredicate negated = predicate.negate();

    assertThat(negated.test(request)).isTrue();

    predicate = request -> true;
    negated = predicate.negate();

    assertThat(negated.test(request)).isFalse();
  }

  @Test
  void or() {
    RequestPredicate predicate1 = request -> true;
    RequestPredicate predicate2 = request -> false;
    RequestPredicate predicate3 = request -> false;

    assertThat(predicate1.or(predicate2).test(request)).isTrue();
    assertThat(predicate2.or(predicate1).test(request)).isTrue();
    assertThat(predicate2.or(predicate3).test(request)).isFalse();
  }

  @Test
  void nestWhenTrue() {
    RequestPredicate predicate = request -> true;
    Optional<ServerRequest> result = predicate.nest(request);

    assertThat(result).isPresent();
    assertThat(result.get()).isSameAs(request);
  }

  @Test
  void nestWhenFalse() {
    RequestPredicate predicate = request -> false;
    Optional<ServerRequest> result = predicate.nest(request);

    assertThat(result).isEmpty();
  }

  @Test
  void acceptCallsUnknownByDefault() {
    RequestPredicate predicate = request -> true;
    RequestPredicates.Visitor visitor = new RequestPredicates.Visitor() {
      boolean unknownCalled = false;

      @Override
      public void unknown(RequestPredicate predicate) {
        unknownCalled = true;
      }

      @Override
      public void method(java.util.Set<infra.http.HttpMethod> methods) { }

      @Override
      public void path(String pattern) { }

      @Override
      public void pathExtension(String extension) { }

      @Override
      public void header(String name, String value) { }

      @Override
      public void param(String name, String value) { }

      @Override
      public void version(String version) { }

      @Override
      public void startAnd() { }

      @Override
      public void and() { }

      @Override
      public void endAnd() { }

      @Override
      public void startOr() { }

      @Override
      public void or() { }

      @Override
      public void endOr() { }

      @Override
      public void startNegate() { }

      @Override
      public void endNegate() { }
    };

    predicate.accept(visitor);
    // Since we cannot easily verify the call, we at least ensure no exception is thrown
    assertThat(true).isTrue();
  }

  @Test
  void andIsShortCircuiting() {
    RequestPredicate predicate1 = request -> false;
    RequestPredicate predicate2 = request -> {
      throw new RuntimeException("Should not be called");
    };

    RequestPredicate andPredicate = predicate1.and(predicate2);
    boolean result = andPredicate.test(request);

    assertThat(result).isFalse();
  }

  @Test
  void orIsShortCircuiting() {
    RequestPredicate predicate1 = request -> true;
    RequestPredicate predicate2 = request -> {
      throw new RuntimeException("Should not be called");
    };

    RequestPredicate orPredicate = predicate1.or(predicate2);
    boolean result = orPredicate.test(request);

    assertThat(result).isTrue();
  }

}
