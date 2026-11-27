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

package infra.web.handler;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.util.Set;

import infra.http.HttpHeaders;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.ServerHttpExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AbstractHandlerExceptionHandlerTests {

  private final ServerHttpExchange exchange = mock(ServerHttpExchange.class);

  private final IllegalStateException failure = new IllegalStateException("cannot render");

  @Test
  void selectsMappedInstancesAndTypesAndDisablesCachingOnlyForMatches() throws Exception {
    var resolver = new TestHandler();
    Object mapped = new Object();
    resolver.setMappedHandlers(Set.of(mapped));
    resolver.addMappedHandlerClass(Number.class);
    resolver.addMappedHandlerClass(CharSequence.class);
    resolver.setPreventResponseCaching(true);

    assertThat(resolver.getMappedHandlerClasses()).containsExactly(Number.class, CharSequence.class);
    assertThat(resolver.handleException(exchange, failure, new Object())).isNull();
    assertThat(resolver.handleException(exchange, failure, null)).isNull();
    verifyNoInteractions(exchange);

    assertThat(resolver.handleException(exchange, failure, mapped)).isEqualTo("error-view");
    verify(exchange).addHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    assertThat(resolver.shouldApplyTo(exchange, 42)).isTrue();
    assertThat(resolver.shouldApplyTo(exchange, "text")).isTrue();
  }

  @Test
  void composesPredicatesAndUsesThemInPreferenceToMappings() throws Exception {
    var resolver = new TestHandler();
    resolver.setMappedHandlerClasses(Number.class);
    resolver.setMappedHandlerPredicate(value -> value instanceof String);
    resolver.setMappedHandlerPredicate("allowed"::equals);

    assertThat(resolver.handleException(exchange, failure, "allowed")).isEqualTo("error-view");
    assertThat(resolver.handleException(exchange, failure, "other")).isNull();
    assertThat(resolver.handleException(exchange, failure, 42)).isNull();
    assertThat(resolver.handleException(exchange, failure, null)).isNull();
  }

  @Test
  void logsResolvedExceptionAtWarnLevelExceptWhenClientDisconnected() throws Exception {
    var resolver = new TestHandler();
    Logger warnLogger = mock(Logger.class);
    given(warnLogger.isWarnEnabled()).willReturn(true);
    try (var factory = mockStatic(LoggerFactory.class)) {
      factory.when(() -> LoggerFactory.getLogger("test-resolver")).thenReturn(warnLogger);
      resolver.setWarnLogCategory("test-resolver");
    }

    assertThat(resolver.handleException(exchange, new EOFException(), null)).isEqualTo("error-view");
    verifyNoInteractions(warnLogger);

    assertThat(resolver.handleException(exchange, failure, null)).isEqualTo("error-view");
    verify(warnLogger).warn("Resolved [java.lang.IllegalStateException: cannot render]");

    resolver.setWarnLogCategory("");
    assertThat(resolver.handleException(exchange, failure, null)).isEqualTo("error-view");
  }

  @Test
  void logsAtDebugWhenWarnIsDisabledAndLogsResolverFailures() throws Exception {
    // Initialize the base class before replacing the factory for the instance logger.
    var resolver = new TestHandler();
    Logger logger = mock(Logger.class);
    Logger warnLogger = mock(Logger.class);
    given(logger.isDebugEnabled()).willReturn(true);
    try (var factory = mockStatic(LoggerFactory.class)) {
      factory.when(() -> LoggerFactory.getLogger(TestHandler.class)).thenReturn(logger);
      factory.when(() -> LoggerFactory.getLogger("disabled-warn")).thenReturn(warnLogger);
      resolver = new TestHandler();
      resolver.setWarnLogCategory("disabled-warn");
    }

    assertThat(resolver.handleException(exchange, failure, "unmapped-by-default")).isEqualTo("error-view");
    verify(logger).debug("Resolved [java.lang.IllegalStateException: cannot render] to error-view");
    verify(warnLogger, never()).warn("Resolved [java.lang.IllegalStateException: cannot render]");

    given(warnLogger.isWarnEnabled()).willReturn(true);
    assertThat(resolver.handleException(exchange, failure, null)).isEqualTo("error-view");
    verify(warnLogger).warn("Resolved [java.lang.IllegalStateException: cannot render]");
    verify(logger).debug("Resolved [java.lang.IllegalStateException: cannot render] to error-view");

    var resolutionFailure = new IllegalArgumentException("view unavailable");
    resolver.logResultedInException(failure, resolutionFailure);

    verify(logger).warn("Failure while trying to resolve exception [{}]",
            IllegalStateException.class.getName(), resolutionFailure);
  }

  @Test
  void checksForExplicitMappingsIncludingPredicateOnly() {
    var resolver = new TestHandler();
    assertThat(resolver.hasHandlerMappings()).isFalse();
    resolver.setMappedHandlerPredicate(candidate -> candidate == null);
    assertThat(resolver.hasHandlerMappings()).isTrue();
    assertThat(resolver.shouldApplyTo(exchange, null)).isTrue();
  }

  private static class TestHandler extends AbstractHandlerExceptionHandler {

    @Override
    protected Object handleInternal(ServerHttpExchange request, @Nullable Object handler, Throwable ex) {
      return "error-view";
    }

  }

}
