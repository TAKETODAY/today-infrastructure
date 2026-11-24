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

package infra.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;

import infra.context.ConfigurableApplicationContext;
import infra.logging.Logger;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

class ApplicationStartupListenersTests {

  @Test
  void reportsListenerFailureWithItsStackTraceInDebugMode() {
    Logger log = mock(Logger.class);
    given(log.isDebugEnabled()).willReturn(true);
    var failure = new IllegalStateException("original failure");
    var listenerFailure = new IllegalArgumentException("failed listener");
    var broken = mock(ApplicationStartupListener.class);
    var next = mock(ApplicationStartupListener.class);
    willThrow(listenerFailure).given(broken).failed(null, failure);

    new ApplicationStartupListeners(log, List.of(broken, next)).failed(null, failure);

    then(log).should().error("Error handling failed", listenerFailure);
    then(next).should().failed(null, failure);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = "listener message")
  void reportsOnlyTheListenerMessageOutsideDebugMode(String message) {
    Logger log = mock(Logger.class);
    var failure = new IllegalStateException("original failure");
    var broken = mock(ApplicationStartupListener.class);
    var next = mock(ApplicationStartupListener.class);
    willThrow(new IllegalArgumentException(message)).given(broken).failed(null, failure);

    new ApplicationStartupListeners(log, List.of(broken, next)).failed(null, failure);

    then(log).should().warn("Error handling failed ({})", message != null ? message : "no error message");
    then(next).should().failed(null, failure);
  }

  @Test
  void doesNotSendStartedOrReadyForAStoppedContext() {
    var context = mock(ConfigurableApplicationContext.class);
    var listener = mock(ApplicationStartupListener.class);
    var listeners = new ApplicationStartupListeners(mock(Logger.class), List.of(listener));

    listeners.started(context, Duration.ZERO);
    listeners.ready(context, Duration.ZERO);

    then(listener).shouldHaveNoInteractions();
  }

}
