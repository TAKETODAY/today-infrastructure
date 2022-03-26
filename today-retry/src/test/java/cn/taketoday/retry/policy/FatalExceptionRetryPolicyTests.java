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

package cn.taketoday.retry.policy;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.retry.RecoveryCallback;
import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.support.DefaultRetryState;
import cn.taketoday.retry.support.RetryTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FatalExceptionRetryPolicyTests {

  @Test
  public void testFatalExceptionWithoutState() throws Throwable {
    MockRetryCallback callback = new MockRetryCallback();
    callback.setExceptionToThrow(new IllegalArgumentException());

    RetryTemplate retryTemplate = new RetryTemplate();

    // Make sure certain exceptions are fatal...
    Map<Class<? extends Throwable>, Boolean> map = new HashMap<Class<? extends Throwable>, Boolean>();
    map.put(IllegalArgumentException.class, false);
    map.put(IllegalStateException.class, false);

    // ... and allow multiple attempts
    SimpleRetryPolicy policy = new SimpleRetryPolicy(3, map);
    retryTemplate.setRetryPolicy(policy);
    RecoveryCallback<String> recoveryCallback = new RecoveryCallback<String>() {
      public String recover(RetryContext context) throws Exception {
        return "bar";
      }
    };

    Object result = null;
    try {
      result = retryTemplate.execute(callback, recoveryCallback);
    }
    catch (IllegalArgumentException e) {
      // We should swallow the exception when recovery is possible
      fail("Did not expect IllegalArgumentException");
    }
    // Callback is called once: the recovery path should also be called
    assertEquals(1, callback.attempts);
    assertEquals("bar", result);
  }

  @Test
  public void testFatalExceptionWithState() throws Throwable {
    MockRetryCallback callback = new MockRetryCallback();
    callback.setExceptionToThrow(new IllegalArgumentException());

    RetryTemplate retryTemplate = new RetryTemplate();

    Map<Class<? extends Throwable>, Boolean> map = new HashMap<Class<? extends Throwable>, Boolean>();
    map.put(IllegalArgumentException.class, false);
    map.put(IllegalStateException.class, false);

    SimpleRetryPolicy policy = new SimpleRetryPolicy(3, map);
    retryTemplate.setRetryPolicy(policy);

    RecoveryCallback<String> recoveryCallback = new RecoveryCallback<String>() {
      public String recover(RetryContext context) throws Exception {
        return "bar";
      }
    };

    Object result = null;
    try {
      retryTemplate.execute(callback, recoveryCallback, new DefaultRetryState("foo"));
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      // If stateful we have to always rethrow. Clients who want special
      // cases have to implement them in the callback
    }
    result = retryTemplate.execute(callback, recoveryCallback, new DefaultRetryState("foo"));
    // Callback is called once: the recovery path should also be called
    assertEquals(1, callback.attempts);
    assertEquals("bar", result);
  }

  private static class MockRetryCallback implements RetryCallback<String, Exception> {

    private int attempts;

    private Exception exceptionToThrow = new Exception();

    public String doWithRetry(RetryContext context) throws Exception {
      this.attempts++;
      // Just barf...
      throw this.exceptionToThrow;
    }

    public void setExceptionToThrow(Exception exceptionToThrow) {
      this.exceptionToThrow = exceptionToThrow;
    }

  }

}
