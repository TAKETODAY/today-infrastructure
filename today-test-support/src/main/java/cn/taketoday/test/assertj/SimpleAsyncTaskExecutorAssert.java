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

package cn.taketoday.test.assertj;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assert;

import java.lang.reflect.Field;

import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.util.ReflectionUtils;

/**
 * AssertJ {@link Assert} for {@link SimpleAsyncTaskExecutor}.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class SimpleAsyncTaskExecutorAssert
    extends AbstractAssert<SimpleAsyncTaskExecutorAssert, SimpleAsyncTaskExecutor> {

  private SimpleAsyncTaskExecutorAssert(SimpleAsyncTaskExecutor actual) {
    super(actual, SimpleAsyncTaskExecutorAssert.class);
  }

  /**
   * Verifies that the actual executor uses platform threads.
   *
   * @return {@code this} assertion object
   * @throws AssertionError if the actual executor doesn't use platform threads
   */
  public SimpleAsyncTaskExecutorAssert usesPlatformThreads() {
    isNotNull();
    if (producesVirtualThreads()) {
      failWithMessage("Expected executor to use platform threads, but it uses virtual threads");
    }
    return this;
  }

  /**
   * Verifies that the actual executor uses virtual threads.
   *
   * @return {@code this} assertion object
   * @throws AssertionError if the actual executor doesn't use virtual threads
   */
  public SimpleAsyncTaskExecutorAssert usesVirtualThreads() {
    isNotNull();
    if (!producesVirtualThreads()) {
      failWithMessage("Expected executor to use virtual threads, but it uses platform threads");
    }
    return this;
  }

  private boolean producesVirtualThreads() {
    Field field = ReflectionUtils.findField(SimpleAsyncTaskExecutor.class, "virtualThreadDelegate");
    if (field == null) {
      throw new IllegalStateException("Field SimpleAsyncTaskExecutor.virtualThreadDelegate not found");
    }
    ReflectionUtils.makeAccessible(field);
    Object virtualThreadDelegate = ReflectionUtils.getField(field, this.actual);
    return virtualThreadDelegate != null;
  }

  /**
   * Creates a new assertion class with the given {@link SimpleAsyncTaskExecutor}.
   *
   * @param actual the {@link SimpleAsyncTaskExecutor}
   * @return the assertion class
   */
  public static SimpleAsyncTaskExecutorAssert assertThat(SimpleAsyncTaskExecutor actual) {
    return new SimpleAsyncTaskExecutorAssert(actual);
  }

}
