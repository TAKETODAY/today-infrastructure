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

package cn.taketoday.util.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import cn.taketoday.lang.NonNull;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/23 14:04
 */
class DefaultExecutorFactoryTests {

  @Test
  void lookup() {
    assertThat(DefaultExecutorFactory.lookup()).isInstanceOf(MyExecutor.class);
    assertThat(Future.defaultExecutor).isInstanceOf(MyExecutor.class);
  }

  static class ExecutorFactory implements DefaultExecutorFactory {

    @Override
    public Executor createExecutor() {
      return new MyExecutor();
    }

  }

  static class MyExecutor implements Executor {
    final ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

    @Override
    public void execute(@NonNull Runnable command) {
      forkJoinPool.execute(command);
    }
  }

}