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

package infra.context.support;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.ApplicationContextException;
import infra.context.SmartLifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class LifecyclePhaseFailureTests {

  private final StandardBeanFactory beans = new StandardBeanFactory();

  private final DefaultLifecycleProcessor processor = new DefaultLifecycleProcessor();

  @Test
  void requiresAConfigurableFactoryAndAnExecutorForConcurrentPhases() {
    assertThatIllegalArgumentException().isThrownBy(() -> processor.setBeanFactory(mock(BeanFactory.class)))
            .withMessageContaining("ConfigurableBeanFactory");
    processor.setConcurrentStartupForPhase(1, 1000);
    assertThatIllegalStateException().isThrownBy(() -> processor.setBeanFactory(beans))
            .withMessageContaining("bootstrapExecutor");
  }

  @Test
  void propagatesTheFailingBeanNameAndStopsPreviouslyStartedPhasesOnRefreshFailure() {
    var started = mock(SmartLifecycle.class);
    var broken = mock(SmartLifecycle.class);
    given(started.isRunning()).willReturn(false, true);
    given(started.isAutoStartup()).willReturn(true);
    given(broken.isAutoStartup()).willReturn(true);
    given(broken.getPhase()).willReturn(1);
    var failure = new IllegalStateException("startup failed");
    willThrow(failure).given(broken).start();
    beans.registerSingleton("started", started);
    beans.registerSingleton("broken", broken);
    beans.setBootstrapExecutor(Runnable::run);
    processor.setConcurrentStartupForPhase(1, 1000);
    processor.setTimeoutsForShutdownPhases(Map.of(0, 0L));
    processor.setBeanFactory(beans);

    assertThatThrownBy(processor::onRefresh).isInstanceOf(ApplicationContextException.class)
            .hasMessage("Failed to start bean 'broken'").hasCause(failure);
    assertThat(processor.isRunning()).isFalse();
    then(started).should().start();
    then(started).should().stop(any(Runnable.class));
  }

  @Test
  void reportsPhaseTimeoutWhenTheExecutorDoesNotRunTheStartupTask() {
    var pending = mock(SmartLifecycle.class);
    given(pending.getPhase()).willReturn(1);
    beans.registerSingleton("pending", pending);
    beans.setBootstrapExecutor(task -> { });
    processor.setConcurrentStartupForPhase(1, 0);
    processor.setBeanFactory(beans);

    assertThatThrownBy(processor::start).isInstanceOf(ApplicationContextException.class)
            .hasMessageContaining("phase 1 within timeout of 0ms").hasCauseInstanceOf(TimeoutException.class);
    assertThat(processor.isRunning()).isFalse();
    then(pending).should(never()).start();
  }

  @Test
  void continuesStoppingOtherBeansWhenAShutdownCallbackThrows() {
    var broken = mock(SmartLifecycle.class);
    var remaining = mock(SmartLifecycle.class);
    given(broken.getPhase()).willReturn(1);
    given(broken.isRunning()).willReturn(true);
    given(remaining.isRunning()).willReturn(true);
    willThrow(new IllegalStateException("shutdown failed")).given(broken).stop(any(Runnable.class));
    beans.registerSingleton("broken", broken);
    beans.registerSingleton("remaining", remaining);
    processor.setTimeoutForShutdownPhase(0, 0);
    processor.setBeanFactory(beans);

    processor.onClose();

    then(broken).should().stop(any(Runnable.class));
    then(remaining).should().stop(any(Runnable.class));
    assertThat(processor.isRunning()).isFalse();
  }

}
