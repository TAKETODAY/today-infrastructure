/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import infra.context.support.GenericApplicationContext;
import infra.core.Ordered;
import infra.web.handler.CompositeHandlerExceptionHandler;
import infra.web.handler.SimpleHandlerExceptionHandler;
import infra.web.handler.function.HandlerFunction;
import infra.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;
import infra.web.mock.MockServerHttpExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.withSettings;

class HandlerStrategyDiscoveryTests {

  @Test
  void findsNamedStrategiesInsteadOfHigherPriorityBeans() {
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      HandlerMapping mapping = mock(HandlerMapping.class);
      HandlerAdapter adapter = mock(HandlerAdapter.class);
      HandlerExceptionHandler exceptionHandler = mock(HandlerExceptionHandler.class);
      context.getBeanFactory().registerSingleton(HandlerMapping.HANDLER_MAPPING_BEAN_NAME, mapping);
      context.getBeanFactory().registerSingleton(HandlerAdapter.HANDLER_ADAPTER_BEAN_NAME, adapter);
      context.getBeanFactory().registerSingleton(HandlerExceptionHandler.BEAN_NAME, exceptionHandler);
      context.getBeanFactory().registerSingleton("preferredMapping", ordered(HandlerMapping.class, -1));
      context.getBeanFactory().registerSingleton("preferredAdapter", ordered(HandlerAdapter.class, -1));
      context.getBeanFactory().registerSingleton("preferredExceptionHandler", ordered(HandlerExceptionHandler.class, -1));
      context.refresh();

      assertThat(HandlerMapping.find(context, false)).isSameAs(mapping);
      assertThat(HandlerAdapter.find(context, false)).isSameAs(adapter);
      assertThat(HandlerExceptionHandler.find(context, false)).isSameAs(exceptionHandler);
    }
  }

  @Test
  void findsSingleMappingAndExceptionHandlerInParentContext() {
    try (GenericApplicationContext parent = new GenericApplicationContext();
            GenericApplicationContext child = new GenericApplicationContext(parent)) {
      HandlerMapping mapping = mock(HandlerMapping.class);
      HandlerExceptionHandler exceptionHandler = mock(HandlerExceptionHandler.class);
      parent.getBeanFactory().registerSingleton("parentMapping", mapping);
      parent.getBeanFactory().registerSingleton("parentExceptionHandler", exceptionHandler);
      parent.refresh();
      child.refresh();

      assertThat(HandlerMapping.find(child)).isSameAs(mapping);
      assertThat(HandlerExceptionHandler.find(child)).isSameAs(exceptionHandler);
    }
  }

  @Test
  void delegatesToHighestPriorityMappingAcrossContexts() throws Exception {
    try (GenericApplicationContext parent = new GenericApplicationContext();
            GenericApplicationContext child = new GenericApplicationContext(parent)) {
      ServerHttpExchange request = mock(ServerHttpExchange.class);
      Object handler = new Object();
      HandlerMapping first = ordered(HandlerMapping.class, -1);
      HandlerMapping second = ordered(HandlerMapping.class, 0);
      given(first.getHandler(request)).willReturn(handler);
      parent.getBeanFactory().registerSingleton("parentMapping", first);
      child.getBeanFactory().registerSingleton("childMapping", second);
      parent.refresh();
      child.refresh();

      assertThat(HandlerMapping.find(child).getHandler(request)).isSameAs(handler);
      then(second).should(never()).getHandler(request);
    }
  }

  @Test
  void delegatesToHighestPrioritySupportingAdapter() throws Exception {
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      ServerHttpExchange request = mock(ServerHttpExchange.class);
      Object handler = new Object();
      HandlerAdapter first = ordered(HandlerAdapter.class, -1);
      HandlerAdapter second = ordered(HandlerAdapter.class, 0);
      given(first.supports(handler)).willReturn(true);
      given(second.supports(handler)).willReturn(true);
      given(first.handle(request, handler)).willReturn("first");
      context.getBeanFactory().registerSingleton("secondAdapter", second);
      context.getBeanFactory().registerSingleton("firstAdapter", first);
      context.refresh();

      assertThat(HandlerAdapter.find(context).handle(request, handler)).isEqualTo("first");
      then(second).should(never()).handle(request, handler);
    }
  }

  @Test
  void delegatesToExceptionHandlersInPriorityOrderUntilResolved() throws Exception {
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      ServerHttpExchange request = mock(ServerHttpExchange.class);
      Throwable failure = new IllegalStateException("handler failed");
      HandlerExceptionHandler first = ordered(HandlerExceptionHandler.class, -1);
      HandlerExceptionHandler second = ordered(HandlerExceptionHandler.class, 0);
      given(second.handleException(request, failure, null)).willReturn(HandlerExceptionHandler.NONE_RETURN_VALUE);
      context.getBeanFactory().registerSingleton("secondExceptionHandler", second);
      context.getBeanFactory().registerSingleton("firstExceptionHandler", first);
      context.refresh();

      assertThat(HandlerExceptionHandler.find(context).handleException(request, failure, null))
              .isSameAs(HandlerExceptionHandler.NONE_RETURN_VALUE);
      var order = inOrder(first, second);
      then(first).should(order).handleException(request, failure, null);
      then(second).should(order).handleException(request, failure, null);
    }
  }

  @Test
  void explicitAdapterListKeepsCallerOrder() throws Exception {
    ServerHttpExchange request = mock(ServerHttpExchange.class);
    Object handler = new Object();
    HandlerAdapter first = ordered(HandlerAdapter.class, 0);
    HandlerAdapter second = ordered(HandlerAdapter.class, -1);
    given(first.supports(handler)).willReturn(true);
    given(first.handle(request, handler)).willReturn("first");

    assertThat(HandlerAdapter.of(List.of(first, second)).handle(request, handler)).isEqualTo("first");
    then(second).shouldHaveNoInteractions();
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void fallsBackToDefaultStrategiesWhenNoBeansMatch(boolean detectAll) throws Exception {
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      context.refresh();

      assertThat(HandlerMapping.find(context, detectAll).getHandler(new MockServerHttpExchange(context))).isNull();
      assertThat(HandlerAdapter.find(context, detectAll).supports(mock(HandlerFunction.class))).isTrue();
      assertThat(HandlerExceptionHandler.find(context, detectAll))
              .isInstanceOfSatisfying(CompositeHandlerExceptionHandler.class, composite ->
                      assertThat(composite.getExceptionHandlers()).extracting(Object::getClass).containsExactly(
                              ExceptionHandlerAnnotationExceptionHandler.class, SimpleHandlerExceptionHandler.class));
    }
  }

  private static <T> T ordered(Class<T> type, int order) {
    T strategy = mock(type, withSettings().extraInterfaces(Ordered.class));
    given(((Ordered) strategy).getOrder()).willReturn(order);
    return strategy;
  }

}
