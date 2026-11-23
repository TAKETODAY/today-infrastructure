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

package infra.test.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Method;
import java.util.List;

import infra.app.logging.LogLevel;
import infra.app.logging.LoggingSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TestContextManagerLifecycleTests {

  private final TestContext context = mock(TestContext.class);

  private final TestExecutionListener listener = mock(TestExecutionListener.class);

  private final TestContextManager manager;

  TestContextManagerLifecycleTests() {
    given(context.getTestClass()).willReturn((Class) getClass());
    TestContextBootstrapper bootstrapper = mock(TestContextBootstrapper.class);
    given(bootstrapper.buildTestContext()).willReturn(context);
    given(bootstrapper.getTestExecutionListeners()).willReturn(List.of(listener));
    manager = new TestContextManager(bootstrapper);
  }

  @ParameterizedTest
  @EnumSource(Callback.class)
  void propagatesListenerFailuresAndResetsInvoker(Callback callback) throws Exception {
    IllegalStateException failure = new IllegalStateException("listener failure");
    callback.fail(listener, context, failure);
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> callback.invoke(manager, this))
            .isSameAs(failure);
    if (callback != Callback.AFTER_CLASS) {
      verify(context).setMethodInvoker(MethodInvoker.DEFAULT_INVOKER);
    }
  }

  @ParameterizedTest
  @EnumSource(Callback.class)
  void propagatesAbortedTestsWithoutWrapping(Callback callback) throws Exception {
    TestAbortedException failure = new TestAbortedException("test skipped");
    callback.fail(listener, context, failure);
    assertThatExceptionOfType(TestAbortedException.class).isThrownBy(() -> callback.invoke(manager, this))
            .isSameAs(failure);
  }

  @Test
  void marksContextUnusedAfterClassAndPropagatesReleaseFailure() {
    given(context.hasApplicationContext()).willReturn(true);
    IllegalStateException failure = new IllegalStateException("release failed");
    willThrow(failure).given(context).markApplicationContextUnused();
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(manager::afterTestClass).isSameAs(failure);
    verify(context).markApplicationContextUnused();
  }

  @Test
  void releaseFailureIsSuppressedBehindListenerFailure() throws Exception {
    given(context.hasApplicationContext()).willReturn(true);
    IllegalStateException listenerFailure = new IllegalStateException("listener failed");
    IllegalStateException releaseFailure = new IllegalStateException("release failed");
    willThrow(listenerFailure).given(listener).afterTestClass(context);
    willThrow(releaseFailure).given(context).markApplicationContextUnused();
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(manager::afterTestClass)
            .isSameAs(listenerFailure).satisfies(ex -> assertThat(ex.getSuppressed()).containsExactly(releaseFailure));
  }

  @ParameterizedTest
  @CsvSource({ "true, OFF", "false, OFF", "true, WARN", "false, WARN", "true, TRACE", "false, TRACE" })
  void loggingLevelsDoNotChangeCallbackFailurePropagation(boolean aborted, LogLevel level) throws Exception {
    LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
    String name = TestContextManager.class.getName();
    LogLevel previous = logging.getLoggerConfiguration(name).getConfiguredLevel();
    logging.setLogLevel(name, level);
    try {
      Exception failure = aborted ? new TestAbortedException("skip") : new IllegalStateException("failure");
      for (Callback callback : Callback.values()) {
        TestExecutionListener listener = mock(TestExecutionListener.class);
        TestContextBootstrapper bootstrapper = mock(TestContextBootstrapper.class);
        given(bootstrapper.buildTestContext()).willReturn(context);
        given(bootstrapper.getTestExecutionListeners()).willReturn(List.of(listener));
        TestContextManager manager = new TestContextManager(bootstrapper);
        callback.fail(listener, context, failure);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> callback.invoke(manager, this)).isSameAs(failure);
      }
    }
    finally {
      logging.setLogLevel(name, previous);
    }
  }

  enum Callback {
    BEFORE_CLASS, PREPARE_INSTANCE, BEFORE_METHOD, BEFORE_EXECUTION, AFTER_EXECUTION, AFTER_METHOD, AFTER_CLASS;

    void fail(TestExecutionListener listener, TestContext context, Exception failure) throws Exception {
      switch (this) {
        case BEFORE_CLASS -> willThrow(failure).given(listener).beforeTestClass(context);
        case PREPARE_INSTANCE -> willThrow(failure).given(listener).prepareTestInstance(context);
        case BEFORE_METHOD -> willThrow(failure).given(listener).beforeTestMethod(context);
        case BEFORE_EXECUTION -> willThrow(failure).given(listener).beforeTestExecution(context);
        case AFTER_EXECUTION -> willThrow(failure).given(listener).afterTestExecution(context);
        case AFTER_METHOD -> willThrow(failure).given(listener).afterTestMethod(context);
        case AFTER_CLASS -> willThrow(failure).given(listener).afterTestClass(context);
      }
    }

    void invoke(TestContextManager manager, Object instance) throws Exception {
      Method method = TestContextManagerLifecycleTests.class.getDeclaredMethod("releaseFailureIsSuppressedBehindListenerFailure");
      switch (this) {
        case BEFORE_CLASS -> manager.beforeTestClass();
        case PREPARE_INSTANCE -> manager.prepareTestInstance(instance);
        case BEFORE_METHOD -> manager.beforeTestMethod(instance, method);
        case BEFORE_EXECUTION -> manager.beforeTestExecution(instance, method);
        case AFTER_EXECUTION -> manager.afterTestExecution(instance, method, null);
        case AFTER_METHOD -> manager.afterTestMethod(instance, method, null);
        case AFTER_CLASS -> manager.afterTestClass();
      }
    }
  }

}
