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