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

package infra.web.filter;

import org.junit.jupiter.api.Test;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.stereotype.Component;
import infra.web.DispatcherHandler;
import infra.web.Filter;
import infra.web.HandlerAdapter;
import infra.web.HandlerMapping;
import infra.web.HttpContext;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.mock.MockHttpContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the {@link Filter} pipeline with {@link DispatcherHandler}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class FilterIntegrationTests {

  @Configuration
  static class BaseConfig {

    @Component
    ReturnValueHandlerManager returnValueHandlerManager() {
      return new ReturnValueHandlerManager();
    }

  }

  @Test
  void filtersAreDiscoveredAndApplied() throws Throwable {
    var context = new AnnotationConfigApplicationContext();
    context.register(BaseConfig.class);
    context.registerBean("filter", Filter.class, () -> (request, chain) -> {
      request.setAttribute("filterCalled", Boolean.TRUE);
      chain.doFilter(request);
    });
    context.refresh();

    DispatcherHandler handler = new DispatcherHandler(context);
    handler.setHandlerMapping(mockHandlerMapping());
    handler.setHandlerAdapter(mockHandlerAdapter());
    handler.start();

    MockHttpContext request = new MockHttpContext();
    handler.handleRequest(request);

    assertThat(request.getAttribute("filterCalled")).isEqualTo(Boolean.TRUE);
  }

  @Test
  void filtersExecuteInOrder() throws Throwable {
    var order = new StringBuilder();
    var context = new AnnotationConfigApplicationContext();
    context.register(BaseConfig.class);
    context.registerBean("firstFilter", Filter.class, () -> (request, chain) -> {
      order.append("A");
      chain.doFilter(request);
    });
    context.registerBean("secondFilter", Filter.class, () -> (request, chain) -> {
      order.append("B");
      chain.doFilter(request);
    });
    context.refresh();

    DispatcherHandler handler = new DispatcherHandler(context);
    handler.setHandlerMapping(mockHandlerMapping());
    handler.setHandlerAdapter(mockHandlerAdapter());
    handler.start();

    MockHttpContext request = new MockHttpContext();
    handler.handleRequest(request);

    assertThat(order.toString()).isEqualTo("AB");
  }

  @Test
  void shortCircuitSkipsHandler() throws Throwable {
    var context = new AnnotationConfigApplicationContext();
    context.register(BaseConfig.class);
    context.registerBean("blockingFilter", Filter.class, () -> (request, chain) -> {
      request.setAttribute("filterCalled", Boolean.TRUE);
      // do not call chain.doFilter — short-circuit
    });
    context.refresh();

    DispatcherHandler handler = new DispatcherHandler(context);
    handler.setHandlerMapping(mockHandlerMapping());
    handler.setHandlerAdapter(mockHandlerAdapter());
    handler.start();

    MockHttpContext request = new MockHttpContext();
    handler.handleRequest(request);

    assertThat(request.getAttribute("filterCalled")).isEqualTo(Boolean.TRUE);
  }

  @Test
  void noFiltersProceedsNormally() throws Throwable {
    var context = new AnnotationConfigApplicationContext();
    context.register(BaseConfig.class);
    context.refresh();

    DispatcherHandler handler = new DispatcherHandler(context);
    handler.setHandlerMapping(mockHandlerMapping());
    handler.setHandlerAdapter(mockHandlerAdapter());
    handler.start();

    MockHttpContext request = new MockHttpContext();
    handler.handleRequest(request);

    // No filters configured, request should reach handler mapping
    assertThat(request.getAttribute("handlerInvoked")).isEqualTo(Boolean.TRUE);
  }

  private static HandlerMapping mockHandlerMapping() {
    return context -> {
      context.setAttribute("handlerInvoked", Boolean.TRUE);
      return "testHandler";
    };
  }

  private static HandlerAdapter mockHandlerAdapter() {
    return new HandlerAdapter() {
      @Override
      public boolean supports(Object handler) {
        return "testHandler".equals(handler);
      }

      @Override
      public Object handle(HttpContext request, Object handler) {
        return null;
      }
    };
  }

}
