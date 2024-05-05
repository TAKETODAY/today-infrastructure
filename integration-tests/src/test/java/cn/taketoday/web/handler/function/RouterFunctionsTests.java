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

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.view.PathPatternsTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
public class RouterFunctionsTests {

  MockHttpResponseImpl mockResponse = new MockHttpResponseImpl();

  private final ServerRequest request = new DefaultServerRequest(
          new MockRequestContext(null, PathPatternsTestUtils.initRequest("GET", "", true), mockResponse), Collections.emptyList());

  @Test
  public void routeMatch() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();

    RequestPredicate requestPredicate = mock(RequestPredicate.class);
    given(requestPredicate.test(request)).willReturn(true);

    RouterFunction<ServerResponse>
            result = RouterFunctions.route(requestPredicate, handlerFunction);
    assertThat(result).isNotNull();

    Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isTrue();
    assertThat(resultHandlerFunction.get()).isEqualTo(handlerFunction);
  }

  @Test
  public void routeNoMatch() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();

    RequestPredicate requestPredicate = mock(RequestPredicate.class);
    given(requestPredicate.test(request)).willReturn(false);

    RouterFunction<ServerResponse> result = RouterFunctions.route(requestPredicate, handlerFunction);
    assertThat(result).isNotNull();

    Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isFalse();
  }

  @Test
  public void nestMatch() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RouterFunction<ServerResponse> routerFunction = request -> Optional.of(handlerFunction);

    RequestPredicate requestPredicate = mock(RequestPredicate.class);
    given(requestPredicate.nest(request)).willReturn(Optional.of(request));

    RouterFunction<ServerResponse> result = RouterFunctions.nest(requestPredicate, routerFunction);
    assertThat(result).isNotNull();

    Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isTrue();
    assertThat(resultHandlerFunction.get()).isEqualTo(handlerFunction);
  }

  @Test
  public void nestNoMatch() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RouterFunction<ServerResponse> routerFunction = request -> Optional.of(handlerFunction);

    RequestPredicate requestPredicate = mock(RequestPredicate.class);
    given(requestPredicate.nest(request)).willReturn(Optional.empty());

    RouterFunction<ServerResponse> result = RouterFunctions.nest(requestPredicate, routerFunction);
    assertThat(result).isNotNull();

    Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isFalse();
  }

  @Test
  public void nestPathVariable() {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RequestPredicate requestPredicate = request -> request.pathVariable("foo").equals("bar");
    RouterFunction<ServerResponse> nestedFunction = RouterFunctions.route(requestPredicate, handlerFunction);

    RouterFunction<ServerResponse> result = RouterFunctions.nest(RequestPredicates.path("/{foo}"), nestedFunction);
    assertThat(result).isNotNull();

    HttpMockRequestImpl servletRequest = new HttpMockRequestImpl("GET", "/bar");

    MockHttpResponseImpl servletResponse = new MockHttpResponseImpl();
    var requestContext = new MockRequestContext(null, servletRequest, servletResponse);
    ServerRequest request = new DefaultServerRequest(requestContext, Collections.emptyList());

    Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
    assertThat(resultHandlerFunction.isPresent()).isTrue();
    assertThat(resultHandlerFunction.get()).isEqualTo(handlerFunction);
  }

}
