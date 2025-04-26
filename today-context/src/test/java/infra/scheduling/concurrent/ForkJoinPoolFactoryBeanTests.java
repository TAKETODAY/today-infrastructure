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