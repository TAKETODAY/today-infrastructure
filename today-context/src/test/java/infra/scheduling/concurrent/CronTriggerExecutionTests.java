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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

import infra.scheduling.support.CronTrigger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class CronTriggerExecutionTests {

  ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

  AtomicInteger count = new AtomicInteger();

  Runnable quick = count::incrementAndGet;

  Runnable slow = () -> {
    count.incrementAndGet();
    try {
      Thread.sleep(1000);
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  };

  @BeforeEach
  void initialize() {
    scheduler.initialize();
  }

  @AfterEach
  void shutdown() {
    scheduler.shutdown();
  }

  @Test
  void forLenientExecutionQuick() throws Exception {
    scheduler.schedule(quick, CronTrigger.forLenientExecution("*/1 * * * * *"));
    Thread.sleep(2000);
    assertThat(count.get()).isEqualTo(2);
  }

  @Test
  void forLenientExecutionSlow() throws Exception {
    scheduler.schedule(slow, CronTrigger.forLenientExecution("*/1 * * * * *"));
    Thread.sleep(2000);
    assertThat(count.get()).isEqualTo(1);
  }

  @Test
  void forFixedExecutionQuick() throws Exception {
    scheduler.schedule(quick, CronTrigger.forFixedExecution("*/1 * * * * *"));
    Thread.sleep(2000);
    assertThat(count.get()).isEqualTo(2);
  }

  @Test
  void forFixedExecutionSlow() throws Exception {
    scheduler.schedule(slow, CronTrigger.forFixedExecution("*/1 * * * * *"));
    Thread.sleep(2000);
    assertThat(count.get()).isEqualTo(2);
  }

  @Test
  void resumeLenientExecution() throws Exception {
    scheduler.schedule(quick, CronTrigger.resumeLenientExecution("*/1 * * * * *",
            Clock.systemDefaultZone().instant().minusSeconds(2)));
    Thread.sleep(1000);
    assertThat(count.get()).isEqualTo(2);
  }

  @Test
  void resumeFixedExecution() throws Exception {
    scheduler.schedule(quick, CronTrigger.resumeFixedExecution("*/1 * * * * *",
            Clock.systemDefaultZone().instant().minusSeconds(2)));
    Thread.sleep(1000);
    assertThat(count.get()).isEqualTo(3);
  }

}
