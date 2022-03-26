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

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.interceptor.MethodInvocationRetryCallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class MethodInvocationRetryListenerSupportTests {

  @Test
  public void testClose() {
    MethodInvocationRetryListenerSupport support = new MethodInvocationRetryListenerSupport();
    try {
      support.close(null, null, null);
    }
    catch (Exception e) {
      fail("Unexpected exception");
    }
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
    MethodInvocationRetryCallback callback = mock(MethodInvocationRetryCallback.class);
    support.close(context, callback, null);

    assertEquals(1, callsOnDoCloseMethod.get());
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
    RetryCallback callback = mock(RetryCallback.class);
    support.close(context, callback, null);

    assertEquals(0, callsOnDoCloseMethod.get());
  }

  @Test
  public void testOnError() {
    MethodInvocationRetryListenerSupport support = new MethodInvocationRetryListenerSupport();
    try {
      support.onError(null, null, null);
    }
    catch (Exception e) {
      fail("Unexpected exception");
    }
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
    MethodInvocationRetryCallback callback = mock(MethodInvocationRetryCallback.class);
    support.onError(context, callback, null);

    assertEquals(1, callsOnDoOnErrorMethod.get());
  }

  @Test
  public void testOpen() {
    MethodInvocationRetryListenerSupport support = new MethodInvocationRetryListenerSupport();
    assertTrue(support.open(null, null));
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
    MethodInvocationRetryCallback callback = mock(MethodInvocationRetryCallback.class);
    assertTrue(support.open(context, callback));

    assertEquals(1, callsOnDoOpenMethod.get());
  }

}
