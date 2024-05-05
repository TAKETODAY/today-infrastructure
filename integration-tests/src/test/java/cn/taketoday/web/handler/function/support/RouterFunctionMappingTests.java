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

package cn.taketoday.web.handler.function.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.web.handler.HandlerExecutionChain;
import cn.taketoday.web.handler.function.HandlerFunction;
import cn.taketoday.web.handler.function.RouterFunction;
import cn.taketoday.web.handler.function.RouterFunctions;
import cn.taketoday.web.handler.function.ServerResponse;
import cn.taketoday.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RouterFunctionMapping}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 */
class RouterFunctionMappingTests {

  private final List<HttpMessageConverter<?>> messageConverters = Collections.emptyList();

  @Test
  void normal() throws Exception {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RouterFunction<ServerResponse> routerFunction = request -> Optional.of(handlerFunction);

    RouterFunctionMapping mapping = new RouterFunctionMapping(routerFunction);
    mapping.setMessageConverters(this.messageConverters);

    MockRequestContext request = createTestRequest("/match");
    HandlerExecutionChain result = (HandlerExecutionChain) mapping.getHandler(request);

    assertThat(result).isNotNull();
    assertThat(result.getRawHandler()).isSameAs(handlerFunction);
  }

  @Test
  void noMatch() throws Exception {
    RouterFunction<ServerResponse> routerFunction = request -> Optional.empty();

    RouterFunctionMapping mapping = new RouterFunctionMapping(routerFunction);
    mapping.setMessageConverters(this.messageConverters);

    MockRequestContext request = createTestRequest("/match");
    Object result = mapping.getHandler(request);

    assertThat(result).isNull();
  }

  @Test
  void empty() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.refresh();

    RouterFunctionMapping mapping = new RouterFunctionMapping();
    mapping.setMessageConverters(this.messageConverters);
    mapping.setApplicationContext(context);
    mapping.afterPropertiesSet();

    MockRequestContext request = createTestRequest("/match");
    Object result = mapping.getHandler(request);

    assertThat(result).isNull();
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void detectHandlerFunctionsInAncestorContexts(boolean detect) throws Exception {
    HandlerFunction<ServerResponse> function1 = request -> ServerResponse.ok().build();
    HandlerFunction<ServerResponse> function2 = request -> ServerResponse.ok().build();
    HandlerFunction<ServerResponse> function3 = request -> ServerResponse.ok().build();

    AnnotationConfigApplicationContext context1 = new AnnotationConfigApplicationContext();
    context1.registerBean("fn1", RouterFunction.class, () -> RouterFunctions.route().GET("/fn1", function1).build());
    context1.refresh();

    AnnotationConfigApplicationContext context2 = new AnnotationConfigApplicationContext();
    context2.registerBean("fn2", RouterFunction.class, () -> RouterFunctions.route().GET("/fn2", function2).build());
    context2.setParent(context1);
    context2.refresh();

    AnnotationConfigApplicationContext context3 = new AnnotationConfigApplicationContext();
    context3.registerBean("fn3", RouterFunction.class, () -> RouterFunctions.route().GET("/fn3", function3).build());
    context3.setParent(context2);
    context3.refresh();

    RouterFunctionMapping mapping = new RouterFunctionMapping();
    mapping.setDetectHandlerFunctionsInAncestorContexts(detect);
    mapping.setMessageConverters(this.messageConverters);
    mapping.setApplicationContext(context3);
    mapping.afterPropertiesSet();

    HandlerExecutionChain chain1 = (HandlerExecutionChain) mapping.getHandler(createTestRequest("/fn1"));
    HandlerExecutionChain chain2 = (HandlerExecutionChain) mapping.getHandler(createTestRequest("/fn2"));
    if (detect) {
      assertThat(chain1).isNotNull().extracting(HandlerExecutionChain::getRawHandler).isSameAs(function1);
      assertThat(chain2).isNotNull().extracting(HandlerExecutionChain::getRawHandler).isSameAs(function2);
    }
    else {
      assertThat(chain1).isNull();
      assertThat(chain2).isNull();
    }

    HandlerExecutionChain chain3 = (HandlerExecutionChain) mapping.getHandler(createTestRequest("/fn3"));
    assertThat(chain3).isNotNull().extracting(HandlerExecutionChain::getRawHandler).isSameAs(function3);

  }

  @Test
  void changeParser() throws Exception {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
            .GET("/foo", handlerFunction)
            .POST("/bar", handlerFunction)
            .build();

    RouterFunctionMapping mapping = new RouterFunctionMapping(routerFunction);
    mapping.setMessageConverters(this.messageConverters);
    mapping.setUseCaseSensitiveMatch(false);
    mapping.afterPropertiesSet();

    MockRequestContext request = createTestRequest("/FOO");
    HandlerExecutionChain result = (HandlerExecutionChain) mapping.getHandler(request);

    assertThat(result).isNotNull();
    assertThat(result.getRawHandler()).isSameAs(handlerFunction);
  }

  @Test
  void mappedRequestShouldHoldAttributes() throws Exception {
    HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
    RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
            .GET("/match", handlerFunction)
            .build();

    RouterFunctionMapping mapping = new RouterFunctionMapping(routerFunction);
    mapping.setMessageConverters(this.messageConverters);

    MockRequestContext request = createTestRequest("/match");
    HandlerExecutionChain result = (HandlerExecutionChain) mapping.getHandler(request);

    assertThat(result).isNotNull();
  }

  private MockRequestContext createTestRequest(String path) {
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", path);
    return new MockRequestContext(null, request, new MockHttpResponseImpl());
  }

}
