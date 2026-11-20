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

import java.lang.reflect.InvocationTargetException;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class StartupExceptionHandlerTests {

  @Test
  void suppressesAnAlreadyLoggedCauseButClearsItAfterHandling() {
    var parent = mock(Thread.UncaughtExceptionHandler.class);
    var handler = new StartupExceptionHandler(parent);
    var cause = new IllegalStateException("startup failed");
    var failure = new InvocationTargetException(cause);
    Thread thread = Thread.currentThread();
    handler.registerLoggedException(cause);
    handler.registerExitCode(0);

    handler.uncaughtException(thread, failure);
    then(parent).shouldHaveNoInteractions();

    handler.uncaughtException(thread, failure);
    then(parent).should().uncaughtException(thread, failure);
  }

  @Test
  void forwardsLogbackConfigurationErrorsEvenIfAlreadyLogged() {
    var parent = mock(Thread.UncaughtExceptionHandler.class);
    var handler = new StartupExceptionHandler(parent);
    var cause = new IllegalStateException("Logback configuration error detected: missing appender");
    var failure = new InvocationTargetException(cause);
    Thread thread = Thread.currentThread();
    handler.registerLoggedException(cause);

    handler.uncaughtException(thread, failure);

    then(parent).should().uncaughtException(thread, failure);
  }

  @Test
  void forwardsExceptionsWithNoMessageOrInvocationCause() {
    var parent = mock(Thread.UncaughtExceptionHandler.class);
    var handler = new StartupExceptionHandler(parent);
    var failure = new InvocationTargetException(null);
    Thread thread = Thread.currentThread();

    handler.uncaughtException(thread, failure);
    handler.uncaughtException(thread, new IllegalStateException());

    then(parent).should().uncaughtException(thread, failure);
    then(parent).should().uncaughtException(org.mockito.ArgumentMatchers.same(thread),
            org.mockito.ArgumentMatchers.isA(IllegalStateException.class));
  }

}
