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

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.core.Ordered;
import infra.stereotype.Component;
import infra.web.DispatcherHandler;
import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.HandlerAdapter;
import infra.web.HandlerMapping;
import infra.web.HttpContext;
import infra.web.HttpRequestHandler;
import infra.web.RequestCompletedListener;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockAsyncContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
  void filterChainIsReusedAcrossRequests() throws Throwable {
    var chains = new ArrayList<FilterChain>();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(BaseConfig.class);
      context.registerBean("filter", Filter.class, () -> (request, chain) -> {
        chains.add(chain);
        request.setAttribute("filterCalled", Boolean.TRUE);
        chain.doFilter(request);
      });
      context.refresh();

      DispatcherHandler handler = new DispatcherHandler(context);
      handler.setHandlerMapping(mockHandlerMapping());
      handler.setHandlerAdapter(mockHandlerAdapter());
      handler.start();

      for (int i = 0; i < 2; i++) {
        MockHttpContext request = new MockHttpContext();
        handler.handleRequest(request);
        assertThat(request.getAttribute("filterCalled")).isEqualTo(Boolean.TRUE);
        assertThat(request.getAttribute("handlerInvoked")).isEqualTo(Boolean.TRUE);
      }

      assertThat(chains).hasSize(2);
      assertThat(chains.get(0)).isSameAs(chains.get(1));
    }
  }

  @Test
  void repeatedContinuationInvokesAllRemainingFilters() throws Throwable {
    var order = new StringBuilder();
    AtomicInteger completions = new AtomicInteger();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(BaseConfig.class);
      context.registerBean("firstFilter", Filter.class, () -> (request, chain) -> {
        order.append("A");
        chain.doFilter(request);
        chain.doFilter(request);
      });
      context.registerBean("secondFilter", Filter.class, () -> (request, chain) -> {
        order.append("B");
        chain.doFilter(request);
        order.append("b");
      });
      context.registerBean("completed", RequestCompletedListener.class,
              () -> (request, notHandled) -> completions.incrementAndGet());
      context.refresh();

      DispatcherHandler handler = new DispatcherHandler(context);
      HttpRequestHandler terminalHandler = request -> {
        order.append("H");
        return HttpRequestHandler.NONE_RETURN_VALUE;
      };
      handler.setHandlerMapping(request -> terminalHandler);
      handler.setHandlerAdapter(mockHandlerAdapter());
      handler.start();

      handler.handleRequest(new MockHttpContext());

      assertThat(order.toString()).isEqualTo("ABHbBHb");
      assertThat(completions).hasValue(1);
    }
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
    assertThat(request.getRequestProcessingTime()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void shortCircuitingFilterCompletesRequestOnce() throws Throwable {
    AtomicInteger completions = new AtomicInteger();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(BaseConfig.class);
      context.registerBean("blockingFilter", Filter.class, () -> (request, chain) -> {
        // do not call chain.doFilter — short-circuit
      });
      context.registerBean("completed", RequestCompletedListener.class,
              () -> (request, notHandled) -> completions.incrementAndGet());
      context.refresh();

      DispatcherHandler handler = new DispatcherHandler(context);
      handler.setHandlerMapping(mockHandlerMapping());
      handler.setHandlerAdapter(mockHandlerAdapter());
      handler.start();

      MockHttpContext request = new MockHttpContext();
      handler.handleRequest(request);

      assertThat(request.isRequestCompleted()).isTrue();
      assertThat(completions).hasValue(1);
    }
  }

  @Test
  void invokingFilterCompletesRequestOnce() throws Throwable {
    AtomicInteger completions = new AtomicInteger();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(BaseConfig.class);
      context.registerBean("passThroughFilter", Filter.class, () -> (request, chain) -> chain.doFilter(request));
      context.registerBean("completed", RequestCompletedListener.class,
              () -> (request, notHandled) -> completions.incrementAndGet());
      context.refresh();

      DispatcherHandler handler = new DispatcherHandler(context);
      handler.setHandlerMapping(mockHandlerMapping());
      handler.setHandlerAdapter(mockHandlerAdapter());
      handler.start();

      MockHttpContext request = new MockHttpContext();
      handler.handleRequest(request);

      assertThat(request.isRequestCompleted()).isTrue();
      assertThat(completions).hasValue(1);
    }
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

  @Test
  void filtersWrapTerminalInRegistrationOrder() throws Throwable {
    var order = new StringBuilder();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(BaseConfig.class);
      context.registerBean("firstFilter", Filter.class, () -> (request, chain) -> {
        order.append("A");
        chain.doFilter(request);
        order.append("a");
      });
      context.registerBean("secondFilter", Filter.class, () -> (request, chain) -> {
        order.append("B");
        chain.doFilter(request);
        order.append("b");
      });
      context.refresh();

      DispatcherHandler handler = new DispatcherHandler(context);
      HttpRequestHandler terminal = request -> {
        order.append("H");
        return HttpRequestHandler.NONE_RETURN_VALUE;
      };
      handler.setHandlerMapping(request -> terminal);
      handler.setHandlerAdapter(mockHandlerAdapter());
      handler.start();

      handler.handleRequest(new MockHttpContext());

      assertThat(order.toString()).isEqualTo("ABHba");
    }
  }

  @Test
  void orderedFiltersExecuteInAscendingOrder() throws Throwable {
    var order = new StringBuilder();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(BaseConfig.class);
      context.registerBean("secondFilter", Filter.class, () -> new OrderedFilter("B", 2, order));
      context.registerBean("firstFilter", Filter.class, () -> new OrderedFilter("A", 1, order));
      context.registerBean("thirdFilter", Filter.class, () -> new OrderedFilter("C", 3, order));
      context.refresh();

      DispatcherHandler handler = new DispatcherHandler(context);
      HttpRequestHandler terminal = request -> {
        order.append("H");
        return HttpRequestHandler.NONE_RETURN_VALUE;
      };
      handler.setHandlerMapping(request -> terminal);
      handler.setHandlerAdapter(mockHandlerAdapter());
      handler.start();

      handler.handleRequest(new MockHttpContext());

      assertThat(order.toString()).isEqualTo("ABCHcba");
    }
  }

  @Test
  void shortCircuitAtMiddleFilterSkipsRemainingFilters() throws Throwable {
    var order = new StringBuilder();
    AtomicInteger completions = new AtomicInteger();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(BaseConfig.class);
      context.registerBean("firstFilter", Filter.class, () -> (request, chain) -> {
        order.append("A");
        chain.doFilter(request);
        order.append("a");
      });
      context.registerBean("secondFilter", Filter.class, () -> (request, chain) -> order.append("B"));
      context.registerBean("thirdFilter", Filter.class, () -> (request, chain) -> {
        order.append("C");
        chain.doFilter(request);
        order.append("c");
      });
      context.registerBean("completed", RequestCompletedListener.class,
              () -> (request, notHandled) -> completions.incrementAndGet());
      context.refresh();

      DispatcherHandler handler = new DispatcherHandler(context);
      HttpRequestHandler terminal = request -> {
        order.append("H");
        return HttpRequestHandler.NONE_RETURN_VALUE;
      };
      handler.setHandlerMapping(request -> terminal);
      handler.setHandlerAdapter(mockHandlerAdapter());
      handler.start();

      handler.handleRequest(new MockHttpContext());

      assertThat(order.toString()).isEqualTo("ABa");
      assertThat(completions).hasValue(1);
    }
  }

  @Test
  void postProcessingFailureDoesNotRepeatCompletion() {
    var failure = new IllegalStateException("post-processing failed");
    var failures = new ArrayList<Throwable>();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(BaseConfig.class);
      context.registerBean("filter", Filter.class, () -> (request, chain) -> {
        chain.doFilter(request);
        assertThat(request.isRequestCompleted()).isTrue();
        throw failure;
      });
      context.registerBean("completed", RequestCompletedListener.class,
              () -> (request, notHandled) -> failures.add(notHandled));
      context.refresh();
      DispatcherHandler handler = new DispatcherHandler(context);
      handler.setHandlerMapping(request -> (HttpRequestHandler) http -> HttpRequestHandler.NONE_RETURN_VALUE);
      handler.setExceptionHandler((request, ex, target) -> {
        throw new AssertionError("Completed requests must not be resolved again");
      });
      handler.start();

      MockHttpContext request = new MockHttpContext();
      assertThatThrownBy(() -> handler.handleRequest(request)).isSameAs(failure);
      assertThat(request.isRequestCompleted()).isTrue();
      assertThat(failures).containsExactly((Throwable) null);
    }
  }

  @Test
  void failingListenerDoesNotChangeSuccessfulResponse() throws Exception {
    var failures = new ArrayList<Throwable>();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(BaseConfig.class);
      context.registerBean("filter", Filter.class, () -> (request, chain) -> {
        request.setStatus(202);
        request.getWriter().write("success");
      });
      context.refresh();
      DispatcherHandler handler = new DispatcherHandler(context);
      handler.start();
      handler.addRequestCompletedActions((request, notHandled) -> {
        throw new IllegalStateException("listener failed");
      }, (request, notHandled) -> failures.add(notHandled));
      MockHttpContext request = new MockHttpContext();

      handler.handleRequest(request);

      assertThat(request.getStatus()).isEqualTo(202);
      assertThat(request.getResponse().getContentAsString()).isEqualTo("success");
      assertThat(request.isRequestCompleted()).isTrue();
      assertThat(failures).containsExactly((Throwable) null);
    }
  }

  @Test
  void failingListenerDoesNotPreventOtherListenersOrCleanup() throws Exception {
    var failure = new IllegalStateException("filter failed");
    var listenerFailure = new IllegalArgumentException("listener failed");
    var completions = new AtomicInteger();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(BaseConfig.class);
      context.registerBean("filter", Filter.class, () -> (request, chain) -> {
        throw failure;
      });
      context.refresh();
      DispatcherHandler handler = new DispatcherHandler(context);
      handler.setExceptionHandler((request, ex, target) -> {
        throw new AssertionError("Committed responses must not be reset");
      });
      handler.start();
      handler.addRequestCompletedActions((request, notHandled) -> {
        throw listenerFailure;
      }, (request, notHandled) -> completions.incrementAndGet());

      MockHttpContext request = new MockHttpContext();
      request.getResponse().setCommitted(true);
      assertThatThrownBy(() -> handler.handleRequest(request)).isSameAs(failure);
      assertThat(request.isRequestCompleted()).isTrue();
      assertThat(failure.getSuppressed()).containsExactly(listenerFailure);
      assertThat(completions).hasValue(1);
      assertThatCode(() -> handler.handleConcurrentResult(request, null, "late result")).doesNotThrowAnyException();
      assertThat(completions).hasValue(1);
    }
  }

  @Test
  void filterFailureIsResolvedBeforeRequestCompletion() throws Exception {
    verifyResolvedFilterFailure(false);
  }

  @Test
  void asyncFilterFailureIsResolvedAfterAsyncCompletion() throws Exception {
    verifyResolvedFilterFailure(true);
  }

  private void verifyResolvedFilterFailure(boolean async) throws Exception {
    var failure = new IllegalStateException("filter failed");
    var failures = new ArrayList<Throwable>();
    var asyncCompletions = new AtomicInteger();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(BaseConfig.class);
      context.registerBean("filter", Filter.class, () -> (request, chain) -> {
        if (async) {
          request.asyncWebRequest().addCompletionHandler(asyncCompletions::incrementAndGet);
          request.asyncWebRequest().startAsync();
        }
        request.setStatus(202);
        throw failure;
      });
      context.registerBean("completed", RequestCompletedListener.class,
              () -> (request, notHandled) -> failures.add(notHandled));
      context.refresh();
      var handler = new DispatcherHandler(context);
      handler.setExceptionHandler((request, ex, target) -> {
        assertThat(ex).isSameAs(failure);
        assertThat(target).isNull();
        assertThat(request.isRequestCompleted()).isFalse();
        assertThat(request.isConcurrentHandlingStarted()).isFalse();
        request.setStatus(400);
        request.getWriter().write("resolved filter failure");
        return HttpRequestHandler.NONE_RETURN_VALUE;
      });
      handler.start();
      var request = new MockHttpContext();
      request.getRequest().setAsyncSupported(true);
      if (async) {
        request.asyncWebRequest().startAsync();
        ((MockAsyncContext) request.getRequest().getAsyncContext()).addDispatchHandler(() -> {
          request.getRequest().setAsyncStarted(false);
          try {
            handler.handleConcurrentResult(request, null,
                    request.getAttribute(infra.web.async.WebAsyncManager.WEB_ASYNC_RESULT_ATTRIBUTE));
            request.getRequest().getAsyncContext().complete();
          }
          catch (Exception ex) {
            throw new IllegalStateException(ex);
          }
        });
      }

      handler.handleRequest(request);

      assertThat(request.getStatus()).isEqualTo(400);
      assertThat(request.getResponse().getContentAsString()).isEqualTo("resolved filter failure");
      assertThat(request.isRequestCompleted()).isTrue();
      assertThat(failures).containsExactly((Throwable) null);
      if (async) {
        assertThat(request.asyncWebRequest().isAsyncComplete()).isTrue();
        request.asyncWebRequest().dispatch("late result");
        handler.handleConcurrentResult(request, null, "late result");
        assertThat(asyncCompletions).hasValue(1);
        assertThat(request.getResponse().getContentAsString()).isEqualTo("resolved filter failure");
        assertThat(failures).containsExactly((Throwable) null);
      }
    }
  }

  @Test
  void unresolvedFilterFailureFallsBackToSendError() {
    verifyUnresolvedFilterFailure(false);
  }

  @Test
  void unresolvedAsyncFilterFailureFallsBackToSendError() {
    verifyUnresolvedFilterFailure(true);
  }

  private void verifyUnresolvedFilterFailure(boolean async) {
    var failure = new IllegalStateException("filter failed");
    var failures = new ArrayList<Throwable>();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(BaseConfig.class);
      context.registerBean("filter", Filter.class, () -> (request, chain) -> {
        if (async) {
          request.asyncWebRequest().startAsync();
        }
        throw failure;
      });
      context.registerBean("completed", RequestCompletedListener.class,
              () -> (request, notHandled) -> failures.add(notHandled));
      context.refresh();

      DispatcherHandler handler = new DispatcherHandler(context);
      handler.setHandlerMapping(mockHandlerMapping());
      handler.setHandlerAdapter(mockHandlerAdapter());
      handler.start();

      MockHttpContext request = new MockHttpContext();
      request.getRequest().setAsyncSupported(true);
      if (async) {
        request.asyncWebRequest().startAsync();
        ((MockAsyncContext) request.getRequest().getAsyncContext()).addDispatchHandler(() -> {
          request.getRequest().setAsyncStarted(false);
          try {
            handler.handleConcurrentResult(request, null,
                    request.getAttribute(infra.web.async.WebAsyncManager.WEB_ASYNC_RESULT_ATTRIBUTE));
            request.getRequest().getAsyncContext().complete();
          }
          catch (Exception ex) {
            throw new IllegalStateException(ex);
          }
        });
      }
      assertThatCode(() -> handler.handleRequest(request)).doesNotThrowAnyException();
      assertThat(request.getStatus()).isEqualTo(500);
      assertThat(request.isRequestCompleted()).isTrue();
      assertThat(failures).containsExactly(failure);
      assertThat(request.isConcurrentHandlingStarted()).isFalse();
      if (async) {
        assertThat(request.asyncWebRequest().isAsyncComplete()).isTrue();
        request.asyncWebRequest().dispatch("late result");
        assertThat(failure.getSuppressed()).isEmpty();
      }
    }
  }

  @Test
  void exceptionInMiddleFilterSkipsOuterPostProcessing() {
    var order = new StringBuilder();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(BaseConfig.class);
      context.registerBean("firstFilter", Filter.class, () -> (request, chain) -> {
        order.append("A");
        chain.doFilter(request);
        order.append("a");
      });
      context.registerBean("secondFilter", Filter.class, () -> (request, chain) -> {
        order.append("B");
        throw new IllegalStateException("filter failed");
      });
      context.registerBean("thirdFilter", Filter.class, () -> (request, chain) -> {
        order.append("C");
        chain.doFilter(request);
      });
      context.refresh();

      DispatcherHandler handler = new DispatcherHandler(context);
      HttpRequestHandler terminal = request -> {
        order.append("H");
        return HttpRequestHandler.NONE_RETURN_VALUE;
      };
      handler.setHandlerMapping(request -> terminal);
      handler.setHandlerAdapter(mockHandlerAdapter());
      handler.start();

      assertThatCode(() -> handler.handleRequest(new MockHttpContext())).doesNotThrowAnyException();

      assertThat(order.toString()).isEqualTo("AB");
    }
  }

  static final class OrderedFilter implements Filter, Ordered {

    private final String name;
    private final int order;
    private final StringBuilder log;

    OrderedFilter(String name, int order, StringBuilder log) {
      this.name = name;
      this.order = order;
      this.log = log;
    }

    @Override
    public void doFilter(HttpContext http, FilterChain chain) throws Exception {
      log.append(name);
      chain.doFilter(http);
      log.append(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public int getOrder() {
      return order;
    }
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
