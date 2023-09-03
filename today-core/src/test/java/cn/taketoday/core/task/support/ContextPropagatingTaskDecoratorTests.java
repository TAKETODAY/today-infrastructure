/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core.task.support;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.context.ThreadLocalAccessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/9/3 12:54
 */
class ContextPropagatingTaskDecoratorTests {

  @Test
  void shouldPropagateContextInTaskExecution() throws Exception {
    AtomicReference<String> actual = new AtomicReference<>("");
    ContextRegistry registry = new ContextRegistry();
    registry.registerThreadLocalAccessor(new TestThreadLocalAccessor());
    ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder().contextRegistry(registry).build();

    Runnable task = () -> actual.set(TestThreadLocalHolder.getValue());
    TestThreadLocalHolder.setValue("expected");

    Thread execution = new Thread(new ContextPropagatingTaskDecorator(snapshotFactory).decorate(task));
    execution.start();
    execution.join();
    assertThat(actual.get()).isEqualTo("expected");
    TestThreadLocalHolder.reset();

    ContextPropagatingTaskDecorator decorator = new ContextPropagatingTaskDecorator();
    assertThat(decorator).extracting("factory").isNotNull();

  }

  static class TestThreadLocalHolder {

    private static final ThreadLocal<String> holder = new ThreadLocal<>();

    public static void setValue(String value) {
      holder.set(value);
    }

    public static String getValue() {
      return holder.get();
    }

    public static void reset() {
      holder.remove();
    }

  }

  static class TestThreadLocalAccessor implements ThreadLocalAccessor<String> {

    public static final String KEY = "test.threadlocal";

    @Override
    public Object key() {
      return KEY;
    }

    @Override
    public String getValue() {
      return TestThreadLocalHolder.getValue();
    }

    @Override
    public void setValue(String value) {
      TestThreadLocalHolder.setValue(value);
    }

    @Override
    public void setValue() {
      TestThreadLocalHolder.reset();
    }

    @Override
    public void restore(String previousValue) {
      setValue(previousValue);
    }

  }

}