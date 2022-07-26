/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.retry.listener;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.interceptor.MethodInvocationRetryCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

public class MethodInvocationRetryListenerSupportTests {

  @Test
  public void testClose() {
    MethodInvocationRetryListenerSupport support = new MethodInvocationRetryListenerSupport();
    assertThatNoException().isThrownBy(() -> support.close(null, null, null));
  }

  @Test
  public void testCloseWithMethodInvocationRetryCallbackShouldCallDoCloseMethod() {
    final AtomicInteger callsOnDoCloseMethod = new AtomicInteger(0);
    MethodInvocationRetryListenerSupport support = new MethodInvocationRetryListenerSupport() {
      @Override
      protected <T, E extends Throwable> void doClose(RetryContext context,
              MethodInvocationRetryCallback<T, E> callback, Throwable throwable) {
        callsOnDoCloseMethod.incrementAndGet();
      }
    };
    RetryContext context = mock(RetryContext.class);
    support.close(context, mockMethodInvocationRetryCallback(), null);

    assertThat(callsOnDoCloseMethod.get()).isEqualTo(1);
  }

  @Test
  public void testCloseWithRetryCallbackShouldntCallDoCloseMethod() {
    final AtomicInteger callsOnDoCloseMethod = new AtomicInteger(0);
    MethodInvocationRetryListenerSupport support = new MethodInvocationRetryListenerSupport() {
      @Override
      protected <T, E extends Throwable> void doClose(RetryContext context,
              MethodInvocationRetryCallback<T, E> callback, Throwable throwable) {
        callsOnDoCloseMethod.incrementAndGet();
      }
    };
    RetryContext context = mock(RetryContext.class);
    RetryCallback<?, ?> callback = mock(RetryCallback.class);
    support.close(context, callback, null);

    assertThat(callsOnDoCloseMethod.get()).isEqualTo(0);
  }

  @Test
  public void testOnError() {
    MethodInvocationRetryListenerSupport support = new MethodInvocationRetryListenerSupport();
    assertThatNoException().isThrownBy(() -> support.onError(null, null, null));
  }

  @Test
  public void testOnErrorWithMethodInvocationRetryCallbackShouldCallDoOnErrorMethod() {
    final AtomicInteger callsOnDoOnErrorMethod = new AtomicInteger(0);
    MethodInvocationRetryListenerSupport support = new MethodInvocationRetryListenerSupport() {
      @Override
      protected <T, E extends Throwable> void doOnError(RetryContext context,
              MethodInvocationRetryCallback<T, E> callback, Throwable throwable) {
        callsOnDoOnErrorMethod.incrementAndGet();
      }
    };
    RetryContext context = mock(RetryContext.class);
    support.onError(context, mockMethodInvocationRetryCallback(), null);

    assertThat(callsOnDoOnErrorMethod.get()).isEqualTo(1);
  }

  @Test
  public void testOpen() {
    MethodInvocationRetryListenerSupport support = new MethodInvocationRetryListenerSupport();
    assertThat(support.open(null, null)).isTrue();
  }

  @Test
  public void testOpenWithMethodInvocationRetryCallbackShouldCallDoCloseMethod() {
    final AtomicInteger callsOnDoOpenMethod = new AtomicInteger(0);
    MethodInvocationRetryListenerSupport support = new MethodInvocationRetryListenerSupport() {
      @Override
      protected <T, E extends Throwable> boolean doOpen(RetryContext context,
              MethodInvocationRetryCallback<T, E> callback) {
        callsOnDoOpenMethod.incrementAndGet();
        return true;
      }
    };
    RetryContext context = mock(RetryContext.class);

    assertThat(support.open(context, mockMethodInvocationRetryCallback())).isTrue();
    assertThat(callsOnDoOpenMethod.get()).isEqualTo(1);
  }

  private MethodInvocationRetryCallback<?, ?> mockMethodInvocationRetryCallback() {
    return mock(MethodInvocationRetryCallback.class);
  }

}
