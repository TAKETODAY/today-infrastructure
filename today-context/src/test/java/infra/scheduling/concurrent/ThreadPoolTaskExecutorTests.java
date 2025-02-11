/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package infra.scheduling.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import infra.core.task.AsyncTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * @author Juergen Hoeller
 * @since 4.0
 */
class ThreadPoolTaskExecutorTests extends AbstractSchedulingTaskExecutorTests {

  private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

  @Override
  protected AsyncTaskExecutor buildExecutor() {
    executor.setThreadNamePrefix(this.threadNamePrefix);
    executor.setMaxPoolSize(1);
    executor.afterPropertiesSet();
    return executor;
  }

  @Test
  void modifyCorePoolSizeWhileRunning() {
    assertThat(executor.getCorePoolSize()).isEqualTo(1);
    assertThat(executor.getThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);

    executor.setCorePoolSize(0);

    assertThat(executor.getCorePoolSize()).isZero();
    assertThat(executor.getThreadPoolExecutor().getCorePoolSize()).isZero();
  }

  @Test
  void modifyCorePoolSizeWithInvalidValueWhileRunning() {
    assertThat(executor.getCorePoolSize()).isEqualTo(1);
    assertThat(executor.getThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);

    assertThatThrownBy(() -> executor.setCorePoolSize(-1))
            .isInstanceOf(IllegalArgumentException.class);

    assertThat(executor.getCorePoolSize()).isEqualTo(1);
    assertThat(executor.getThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
  }

  @Test
  void modifyMaxPoolSizeWhileRunning() {
    assertThat(executor.getMaxPoolSize()).isEqualTo(1);
    assertThat(executor.getThreadPoolExecutor().getMaximumPoolSize()).isEqualTo(1);

    executor.setMaxPoolSize(5);

    assertThat(executor.getMaxPoolSize()).isEqualTo(5);
    assertThat(executor.getThreadPoolExecutor().getMaximumPoolSize()).isEqualTo(5);
  }

  @Test
  void modifyMaxPoolSizeWithInvalidValueWhileRunning() {
    assertThat(executor.getMaxPoolSize()).isEqualTo(1);
    assertThat(executor.getThreadPoolExecutor().getMaximumPoolSize()).isEqualTo(1);

    assertThatThrownBy(() -> executor.setMaxPoolSize(0))
            .isInstanceOf(IllegalArgumentException.class);

    assertThat(executor.getMaxPoolSize()).isEqualTo(1);
    assertThat(executor.getThreadPoolExecutor().getMaximumPoolSize()).isEqualTo(1);
  }

  @Test
  void modifyKeepAliveSecondsWhileRunning() {
    assertThat(executor.getKeepAliveSeconds()).isEqualTo(60);
    assertThat(executor.getThreadPoolExecutor().getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(60);

    executor.setKeepAliveSeconds(10);

    assertThat(executor.getKeepAliveSeconds()).isEqualTo(10);
    assertThat(executor.getThreadPoolExecutor().getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(10);
  }

  @Test
  void modifyKeepAliveSecondsWithInvalidValueWhileRunning() {
    assertThat(executor.getKeepAliveSeconds()).isEqualTo(60);
    assertThat(executor.getThreadPoolExecutor().getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(60);

    assertThatThrownBy(() -> executor.setKeepAliveSeconds(-10))
            .isInstanceOf(IllegalArgumentException.class);

    assertThat(executor.getKeepAliveSeconds()).isEqualTo(60);
    assertThat(executor.getThreadPoolExecutor().getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(60);
  }

  @Test
  void queueCapacityDefault() {
    assertThat(executor.getQueueCapacity()).isEqualTo(Integer.MAX_VALUE);
    assertThat(executor.getThreadPoolExecutor().getQueue())
            .asInstanceOf(type(LinkedBlockingQueue.class))
            .extracting(BlockingQueue::remainingCapacity).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  void queueCapacityZero() {
    executor.setQueueCapacity(0);
    executor.afterPropertiesSet();

    assertThat(executor.getQueueCapacity()).isZero();
    assertThat(executor.getThreadPoolExecutor().getQueue())
            .asInstanceOf(type(SynchronousQueue.class))
            .extracting(BlockingQueue::remainingCapacity).isEqualTo(0);
  }

  @Test
  void queueSize() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    assertThatIllegalStateException().isThrownBy(executor::getThreadPoolExecutor);
    assertThat(executor.getQueueSize()).isZero();

    executor.afterPropertiesSet();

    assertThat(executor.getThreadPoolExecutor()).isNotNull();
    assertThat(executor.getThreadPoolExecutor().getQueue()).isEmpty();
    assertThat(executor.getQueueSize()).isZero();
  }

}
