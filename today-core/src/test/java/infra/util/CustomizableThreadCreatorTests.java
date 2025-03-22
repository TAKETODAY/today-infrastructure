/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 19:24
 */
class CustomizableThreadCreatorTests {

  @Test
  void defaultThreadNamePrefixUsedWhenNullProvided() {
    var creator = new CustomizableThreadCreator(null);
    assertThat(creator.getThreadNamePrefix()).isEqualTo("CustomizableThreadCreator-");
  }

  @Test
  void threadNamePrefixIsAppliedToCreatedThread() {
    var creator = new CustomizableThreadCreator("test-");
    Thread thread = creator.createThread(() -> { });
    assertThat(thread.getName()).startsWith("test-").endsWith("1");
  }

  @Test
  void threadCountIncrementsWithEachCreatedThread() {
    var creator = new CustomizableThreadCreator();
    Thread first = creator.createThread(() -> { });
    Thread second = creator.createThread(() -> { });

    assertThat(first.getName()).endsWith("1");
    assertThat(second.getName()).endsWith("2");
  }

  @Test
  void threadPriorityIsAppliedToCreatedThread() {
    var creator = new CustomizableThreadCreator();
    creator.setThreadPriority(Thread.MAX_PRIORITY);
    Thread thread = creator.createThread(() -> { });
    assertThat(thread.getPriority()).isEqualTo(Thread.MAX_PRIORITY);
  }

  @Test
  void daemonStatusIsAppliedToCreatedThread() {
    var creator = new CustomizableThreadCreator();
    creator.setDaemon(true);
    Thread thread = creator.createThread(() -> { });
    assertThat(thread.isDaemon()).isTrue();
  }

  @Test
  void threadGroupNameCreatesNewThreadGroup() {
    var creator = new CustomizableThreadCreator();
    creator.setThreadGroupName("test-group");
    Thread thread = creator.createThread(() -> { });
    assertThat(thread.getThreadGroup().getName()).isEqualTo("test-group");
  }

  @Test
  void existingThreadGroupIsUsed() {
    ThreadGroup group = new ThreadGroup("existing-group");
    var creator = new CustomizableThreadCreator();
    creator.setThreadGroup(group);
    Thread thread = creator.createThread(() -> { });
    assertThat(thread.getThreadGroup()).isSameAs(group);
  }

  @Test
  void defaultValuesAreCorrect() {
    var creator = new CustomizableThreadCreator();
    assertThat(creator.getThreadPriority()).isEqualTo(Thread.NORM_PRIORITY);
    assertThat(creator.isDaemon()).isFalse();
    assertThat(creator.getThreadGroup()).isNull();
  }

  @Test
  void runnableIsExecutedByCreatedThread() {
    var executed = new AtomicBoolean(false);
    var creator = new CustomizableThreadCreator();

    Thread thread = creator.createThread(() -> executed.set(true));
    thread.start();

    await().atMost(1, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(executed).isTrue());
  }

  @Test
  void customThreadNamePrefixIsRetained() {
    var creator = new CustomizableThreadCreator("custom-");
    assertThat(creator.getThreadNamePrefix()).isEqualTo("custom-");
  }

  @Test
  void setThreadNamePrefixUpdatesPrefixForNewThreads() {
    var creator = new CustomizableThreadCreator("old-");
    Thread first = creator.createThread(() -> { });

    creator.setThreadNamePrefix("new-");
    Thread second = creator.createThread(() -> { });

    assertThat(first.getName()).startsWith("old-");
    assertThat(second.getName()).startsWith("new-");
  }

  @Test
  void threadGroupNullByDefault() {
    var creator = new CustomizableThreadCreator();
    Thread thread = creator.createThread(() -> { });
    assertThat(thread.getThreadGroup()).isEqualTo(Thread.currentThread().getThreadGroup());
  }

  @Test
  void multipleThreadsInSameGroup() {
    var creator = new CustomizableThreadCreator();
    creator.setThreadGroupName("shared-group");

    Thread first = creator.createThread(() -> { });
    Thread second = creator.createThread(() -> { });

    assertThat(first.getThreadGroup()).isSameAs(second.getThreadGroup());
  }

  @Test
  void threadContextClassLoaderInherited() {
    var creator = new CustomizableThreadCreator();
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

    Thread thread = creator.createThread(() -> { });
    assertThat(thread.getContextClassLoader()).isSameAs(contextClassLoader);
  }

}