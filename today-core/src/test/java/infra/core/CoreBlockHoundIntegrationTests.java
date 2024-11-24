/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import infra.core.ReactiveAdapterRegistry;
import infra.util.ConcurrentReferenceHashMap;
import reactor.blockhound.BlockHound;
import reactor.core.scheduler.ReactorBlockHoundIntegration;
import reactor.core.scheduler.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.condition.JRE.JAVA_18;

/**
 * Tests to verify the core BlockHound integration rules.
 *
 * @author Rossen Stoyanchev
 */
@DisabledForJreRange(min = JAVA_18, disabledReason = "BlockHound is not compatible with Java 18+")
class CoreBlockHoundIntegrationTests {

  @BeforeAll
  static void setup() {
    BlockHound.builder()
            .with(new ReactorBlockHoundIntegration())  // Reactor non-blocking thread predicate
            .with(new ReactiveAdapterRegistry.CoreBlockHoundIntegration())
            .install();
  }

  @Test
  void blockHoundIsInstalled() {
    assertThatThrownBy(() -> testNonBlockingTask(() -> Thread.sleep(10)))
            .hasMessageContaining("Blocking call!");
  }

  @Test
  void concurrentReferenceHashMapSegmentDoTask() {
    int size = 10000;
    Map<String, String> map = new ConcurrentReferenceHashMap<>(size);

    CompletableFuture<Object> future1 = new CompletableFuture<>();
    testNonBlockingTask(() -> {
      for (int i = 0; i < size / 2; i++) {
        map.put("a" + i, "bar");
      }
    }, future1);

    CompletableFuture<Object> future2 = new CompletableFuture<>();
    testNonBlockingTask(() -> {
      for (int i = 0; i < size / 2; i++) {
        map.put("b" + i, "bar");
      }
    }, future2);

    CompletableFuture.allOf(future1, future2).join();
    assertThat(map).hasSize(size);
  }

  @Test
  void concurrentReferenceHashMapSegmentClear() {
    int size = 10000;
    Map<String, String> map = new ConcurrentReferenceHashMap<>(size);

    CompletableFuture<Object> future1 = new CompletableFuture<>();
    testNonBlockingTask(() -> {
      for (int i = 0; i < size / 2; i++) {
        map.put("a" + i, "bar");
      }
    }, future1);

    CompletableFuture<Object> future2 = new CompletableFuture<>();
    testNonBlockingTask(() -> {
      for (int i = 0; i < size; i++) {
        map.clear();
      }
    }, future2);

    CompletableFuture.allOf(future1, future2).join();
    assertThat(map).isEmpty();
  }

  private void testNonBlockingTask(NonBlockingTask task) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    testNonBlockingTask(task, future);
    future.join();
  }

  private void testNonBlockingTask(NonBlockingTask task, CompletableFuture<Object> future) {
    Schedulers.parallel().schedule(() -> {
      try {
        task.run();
        future.complete(null);
      }
      catch (Throwable ex) {
        future.completeExceptionally(ex);
      }
    });
  }

  @FunctionalInterface
  private interface NonBlockingTask {

    void run() throws Exception;
  }

}
