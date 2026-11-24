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

package infra.scheduling.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.task.TaskRejectedException;
import infra.scheduling.SchedulingAwareRunnable;
import infra.scheduling.TaskScheduler;
import infra.scheduling.Trigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class TaskSchedulerRouterTests {

  private static final Instant START = Instant.parse("2099-01-01T00:00:00Z");

  private static final Duration INTERVAL = Duration.ofSeconds(2);

  private final StandardBeanFactory beans = new StandardBeanFactory();

  private final TaskSchedulerRouter router = new TaskSchedulerRouter();

  @AfterEach
  void destroyRouter() {
    router.destroy();
  }

  @Test
  void resolvesPlaceholdersInQualifiersAndDelegatesAllSchedulingModes() {
    var defaultScheduler = mock(TaskScheduler.class);
    var qualifiedScheduler = mock(TaskScheduler.class);
    var task = mock(SchedulingAwareRunnable.class);
    given(task.getQualifier()).willReturn("${scheduler}");
    beans.addEmbeddedValueResolver(value -> value.equals("${scheduler}") ? "qualified" : value);
    beans.registerSingleton("taskScheduler", defaultScheduler);
    beans.registerSingleton("qualified", qualifiedScheduler);
    router.setBeanFactory(beans);
    Trigger trigger = context -> START;

    router.schedule(task, trigger);
    router.schedule(task, START);
    router.scheduleAtFixedRate(task, START, INTERVAL);
    router.scheduleAtFixedRate(task, INTERVAL);
    router.scheduleWithFixedDelay(task, START, INTERVAL);
    router.scheduleWithFixedDelay(task, INTERVAL);

    then(qualifiedScheduler).should().schedule(task, trigger);
    then(qualifiedScheduler).should().schedule(task, START);
    then(qualifiedScheduler).should().scheduleAtFixedRate(task, START, INTERVAL);
    then(qualifiedScheduler).should().scheduleAtFixedRate(task, INTERVAL);
    then(qualifiedScheduler).should().scheduleWithFixedDelay(task, START, INTERVAL);
    then(qualifiedScheduler).should().scheduleWithFixedDelay(task, INTERVAL);
    then(defaultScheduler).shouldHaveNoInteractions();
  }

  @Test
  void recordsDependencyOnTheUniqueSchedulerAndKeepsUsingItAfterAnotherIsRegistered() {
    var scheduler = mock(TaskScheduler.class);
    var task = mock(Runnable.class);
    beans.registerSingleton("primary", scheduler);
    router.setBeanName("router");
    router.setBeanFactory(beans);

    router.schedule(task, START);
    var other = mock(TaskScheduler.class);
    beans.registerSingleton("taskScheduler", other);
    router.scheduleWithFixedDelay(task, INTERVAL);

    then(scheduler).should().schedule(task, START);
    then(scheduler).should().scheduleWithFixedDelay(task, INTERVAL);
    then(other).shouldHaveNoInteractions();
    assertThat(beans.getDependentBeans("primary")).containsExactly("router");
  }

  @Test
  void defaultsToTheConventionalBeanNameWhenSchedulerTypeIsAmbiguous() {
    var conventional = mock(TaskScheduler.class);
    var other = mock(TaskScheduler.class);
    var task = mock(Runnable.class);
    beans.registerSingleton("taskScheduler", conventional);
    beans.registerSingleton("otherScheduler", other);
    router.setBeanName("router");
    router.setBeanFactory(beans);

    router.schedule(task, START);

    then(conventional).should().schedule(task, START);
    then(other).shouldHaveNoInteractions();
    assertThat(beans.getDependentBeans("taskScheduler")).containsExactly("router");
  }

  @Test
  void usesQualifiedExecutorsAndFallsBackToTheConventionalExecutorAsDefault() {
    var conventional = mock(ScheduledExecutorService.class);
    var qualified = mock(ScheduledExecutorService.class);
    var task = mock(SchedulingAwareRunnable.class);
    given(task.getQualifier()).willReturn("qualified");
    beans.registerSingleton("taskScheduler", conventional);
    beans.registerSingleton("qualified", qualified);
    router.setBeanFactory(beans);

    router.schedule(task, START);
    router.schedule(() -> { }, START);
    router.destroy();

    then(qualified).should().schedule(any(Runnable.class), anyLong(), eq(TimeUnit.NANOSECONDS));
    then(conventional).should().schedule(any(Runnable.class), anyLong(), eq(TimeUnit.NANOSECONDS));
    then(qualified).shouldHaveNoMoreInteractions();
    then(conventional).shouldHaveNoMoreInteractions();
  }

  @Test
  void shutsDownItsOwnExecutorWhenNoSchedulerCanBeResolved() {
    router.setBeanFactory(beans);
    var future = router.schedule(() -> { }, START);

    router.destroy();

    future.cancel(false);
    assertThatThrownBy(() -> router.schedule(() -> { }, START)).isInstanceOf(TaskRejectedException.class);
  }

  @Test
  void usesTypeLookupOnANonConfigurableBeanFactory() {
    var factory = mock(BeanFactory.class);
    var scheduler = mock(TaskScheduler.class);
    var task = mock(Runnable.class);
    given(factory.getBean(TaskScheduler.class)).willReturn(scheduler);
    router.setBeanFactory(factory);

    router.schedule(task, START);

    then(scheduler).should().schedule(task, START);
  }

  @Test
  void requiresBeanFactoryForBothDefaultAndQualifiedLookups() {
    var task = mock(SchedulingAwareRunnable.class);
    given(task.getQualifier()).willReturn("qualified");

    assertThatIllegalStateException().isThrownBy(() -> router.schedule(() -> { }, START))
            .withMessageContaining("default scheduler");
    assertThatIllegalStateException().isThrownBy(() -> router.schedule(task, START))
            .withMessageContaining("qualified scheduler");
  }

}
