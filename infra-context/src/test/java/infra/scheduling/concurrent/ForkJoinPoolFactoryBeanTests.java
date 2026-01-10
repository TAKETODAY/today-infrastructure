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

package infra.scheduling.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ForkJoinPool;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/28 19:59
 */
class ForkJoinPoolFactoryBeanTests {

  @Test
  void createsCustomForkJoinPoolWithDefaultSettings() throws Exception {
    ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
    factory.afterPropertiesSet();

    ForkJoinPool pool = factory.getObject();
    assertThat(pool).isNotNull();
    assertThat(pool.getParallelism()).isEqualTo(Runtime.getRuntime().availableProcessors());
    assertThat(pool.getAsyncMode()).isFalse();
  }

  @Test
  void createsCommonPoolWhenEnabled() throws Exception {
    ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
    factory.setCommonPool(true);
    factory.afterPropertiesSet();

    assertThat(factory.getObject()).isSameAs(ForkJoinPool.commonPool());
  }

  @Test
  void createsPoolWithCustomParallelism() throws Exception {
    ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
    factory.setParallelism(4);
    factory.afterPropertiesSet();

    ForkJoinPool pool = factory.getObject();
    assertThat(pool.getParallelism()).isEqualTo(4);
  }

  @Test
  void createsPoolWithAsyncMode() throws Exception {
    ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
    factory.setAsyncMode(true);
    factory.afterPropertiesSet();

    ForkJoinPool pool = factory.getObject();
    assertThat(pool.getAsyncMode()).isTrue();
  }

  @Test
  void createsPoolWithCustomThreadFactory() throws Exception {
    ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory = pool -> null;

    ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
    factory.setThreadFactory(threadFactory);
    factory.afterPropertiesSet();

    assertThat(factory.getObject()).isNotNull();
  }

  @Test
  void createsPoolWithUncaughtExceptionHandler() throws Exception {
    Thread.UncaughtExceptionHandler handler = (t, e) -> { };

    ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
    factory.setUncaughtExceptionHandler(handler);
    factory.afterPropertiesSet();

    assertThat(factory.getObject()).isNotNull();
  }

  @Test
  void shutdownPoolOnDestroy() throws Exception {
    ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
    factory.afterPropertiesSet();
    ForkJoinPool pool = factory.getObject();

    factory.destroy();
    assertThat(pool.isShutdown()).isTrue();
  }

  @Test
  void waitsForTerminationOnDestroy() throws Exception {
    ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
    factory.setAwaitTerminationSeconds(1);
    factory.afterPropertiesSet();
    ForkJoinPool pool = factory.getObject();

    pool.submit(() -> {
      try {
        Thread.sleep(500);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });

    factory.destroy();
    assertThat(pool.isTerminated()).isTrue();
  }

  @Test
  void isSingleton() {
    ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
    assertThat(factory.isSingleton()).isTrue();
  }

  @Test
  void returnsCorrectObjectType() {
    ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
    assertThat(factory.getObjectType()).isEqualTo(ForkJoinPool.class);
  }

  @Test
  void doesNotShutdownCommonPoolOnDestroy() throws Exception {
    ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
    factory.setCommonPool(true);
    factory.afterPropertiesSet();
    ForkJoinPool pool = factory.getObject();

    factory.destroy();
    assertThat(pool.isShutdown()).isFalse();
  }

  @Test
  void interruptedExceptionDuringTerminationRestoresFlag() throws Exception {
    ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
    factory.setAwaitTerminationSeconds(1);
    factory.afterPropertiesSet();

    Thread.currentThread().interrupt();
    factory.destroy();

    assertThat(Thread.interrupted()).isTrue();
  }

  @Test
  void returnsNullWhenNotInitialized() {
    ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
    assertThat(factory.getObject()).isNull();
  }

  @Test
  void waitsFullTimeOnTerminationWithLongRunningTask() throws Exception {
    ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
    factory.setAwaitTerminationSeconds(2);
    factory.afterPropertiesSet();
    ForkJoinPool pool = factory.getObject();

    pool.submit(() -> {
      try {
        Thread.sleep(3000);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });

    long start = System.currentTimeMillis();
    factory.destroy();
    long duration = System.currentTimeMillis() - start;

    assertThat(duration).isGreaterThanOrEqualTo(2000);
    assertThat(pool.isShutdown()).isTrue();
  }

}