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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.web.view.PathPatternsTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class RequestPredicateTests {

  private ServerRequest request;

  @BeforeEach
  void createRequest() {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    ServletRequestContext requestContext = new ServletRequestContext(null, servletRequest, new MockHttpServletResponse());
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

}
