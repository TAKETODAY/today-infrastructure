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

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.retry.RecoveryCallback;
import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.support.DefaultRetryState;
import cn.taketoday.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

public class FatalExceptionRetryPolicyTests {

  @Test
  public void testFatalExceptionWithoutState() throws Throwable {
    MockRetryCallback callback = new MockRetryCallback();
    callback.setExceptionToThrow(new IllegalArgumentException());

    RetryTemplate retryTemplate = new RetryTemplate();

    // Make sure certain exceptions are fatal...
    Map<Class<? extends Throwable>, Boolean> map = new HashMap<>();
    map.put(IllegalArgumentException.class, false);
    map.put(IllegalStateException.class, false);

    // ... and allow multiple attempts
    SimpleRetryPolicy policy = new SimpleRetryPolicy(3, map);
    retryTemplate.setRetryPolicy(policy);
    RecoveryCallback<String> recoveryCallback = context -> "bar";

    AtomicReference<Object> result = new AtomicReference<>();
    assertThatNoException().isThrownBy(() -> result.set(retryTemplate.execute(callback, recoveryCallback)));
    // Callback is called once: the recovery path should also be called
    assertThat(callback.attempts).isEqualTo(1);
    assertThat(result.get()).isEqualTo("bar");
  }

  @Test
  public void testFatalExceptionWithState() throws Throwable {
    MockRetryCallback callback = new MockRetryCallback();
    callback.setExceptionToThrow(new IllegalArgumentException());

    RetryTemplate retryTemplate = new RetryTemplate();

    Map<Class<? extends Throwable>, Boolean> map = new HashMap<>();
    map.put(IllegalArgumentException.class, false);
    map.put(IllegalStateException.class, false);

    SimpleRetryPolicy policy = new SimpleRetryPolicy(3, map);
    retryTemplate.setRetryPolicy(policy);

    RecoveryCallback<String> recoveryCallback = context -> "bar";

    Object result = null;
    assertThatIllegalArgumentException()
            .isThrownBy(() -> retryTemplate.execute(callback, recoveryCallback, new DefaultRetryState("foo")));
    result = retryTemplate.execute(callback, recoveryCallback, new DefaultRetryState("foo"));
    // Callback is called once: the recovery path should also be called
    assertThat(callback.attempts).isEqualTo(1);
    assertThat(result).isEqualTo("bar");
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
