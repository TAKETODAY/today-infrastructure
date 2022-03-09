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

package cn.taketoday.scheduling.annotation;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import cn.taketoday.aop.interceptor.AsyncUncaughtExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An {@link AsyncUncaughtExceptionHandler} implementation used for testing purposes.
 *
 * @author Stephane Nicoll
 */
class TestableAsyncUncaughtExceptionHandler
        implements AsyncUncaughtExceptionHandler {

  private final CountDownLatch latch = new CountDownLatch(1);

  private UncaughtExceptionDescriptor descriptor;

  private final boolean throwUnexpectedException;

  TestableAsyncUncaughtExceptionHandler() {
    this(false);
  }

  TestableAsyncUncaughtExceptionHandler(boolean throwUnexpectedException) {
    this.throwUnexpectedException = throwUnexpectedException;
  }

  @Override
  public void handleUncaughtException(Throwable ex, Method method, Object... params) {
    descriptor = new UncaughtExceptionDescriptor(ex, method);
    this.latch.countDown();
    if (throwUnexpectedException) {
      throw new IllegalStateException("Test exception");
    }
  }

  public boolean isCalled() {
    return descriptor != null;
  }

  public void assertCalledWith(Method expectedMethod, Class<? extends Throwable> expectedExceptionType) {
    assertThat(descriptor).as("Handler not called").isNotNull();
    assertThat(descriptor.ex.getClass()).as("Wrong exception type").isEqualTo(expectedExceptionType);
    assertThat(descriptor.method).as("Wrong method").isEqualTo(expectedMethod);
  }

  public void await(long timeout) {
    try {
      this.latch.await(timeout, TimeUnit.MILLISECONDS);
    }
    catch (Exception e) {
      Thread.currentThread().interrupt();
    }
  }

  private static class UncaughtExceptionDescriptor {
    private final Throwable ex;

    private final Method method;

    private UncaughtExceptionDescriptor(Throwable ex, Method method) {
      this.ex = ex;
      this.method = method;
    }
  }
}
