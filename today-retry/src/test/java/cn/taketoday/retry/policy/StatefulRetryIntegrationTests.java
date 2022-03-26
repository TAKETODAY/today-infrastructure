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

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.retry.ExhaustedRetryException;
import cn.taketoday.retry.RecoveryCallback;
import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryState;
import cn.taketoday.retry.backoff.ExponentialBackOffPolicy;
import cn.taketoday.retry.support.DefaultRetryState;
import cn.taketoday.retry.support.RetryTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Dave Syer
 * @author Gary Russell
 */
public class StatefulRetryIntegrationTests {

  @Test
  public void testExternalRetryWithFailAndNoRetry() throws Throwable {
    MockRetryCallback callback = new MockRetryCallback();

    RetryState retryState = new DefaultRetryState("foo");

    RetryTemplate retryTemplate = new RetryTemplate();
    MapRetryContextCache cache = new MapRetryContextCache();
    retryTemplate.setRetryContextCache(cache);
    retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1));

    assertFalse(cache.containsKey("foo"));

    try {
      retryTemplate.execute(callback, retryState);
      // The first failed attempt we expect to retry...
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
      assertEquals(null, e.getMessage());
    }

    assertTrue(cache.containsKey("foo"));

    try {
      retryTemplate.execute(callback, retryState);
      // We don't get a second attempt...
      fail("Expected ExhaustedRetryException");
    }
    catch (ExhaustedRetryException e) {
      // This is now the "exhausted" message:
      assertNotNull(e.getMessage());
    }

    assertFalse(cache.containsKey("foo"));

    // Callback is called once: the recovery path should be called in
    // handleRetryExhausted (so not in this test)...
    assertEquals(1, callback.attempts);
  }

  @Test
  public void testExternalRetryWithSuccessOnRetry() throws Throwable {
    MockRetryCallback callback = new MockRetryCallback();

    RetryState retryState = new DefaultRetryState("foo");

    RetryTemplate retryTemplate = new RetryTemplate();
    MapRetryContextCache cache = new MapRetryContextCache();
    retryTemplate.setRetryContextCache(cache);
    retryTemplate.setRetryPolicy(new SimpleRetryPolicy(2));

    assertFalse(cache.containsKey("foo"));

    Object result = "start_foo";
    try {
      result = retryTemplate.execute(callback, retryState);
      // The first failed attempt we expect to retry...
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
      assertNull(e.getMessage());
    }

    assertTrue(cache.containsKey("foo"));

    result = retryTemplate.execute(callback, retryState);

    assertFalse(cache.containsKey("foo"));

    assertEquals(2, callback.attempts);
    assertEquals(1, callback.context.getRetryCount());
    assertEquals("bar", result);
  }

  @Test
  public void testExternalRetryWithSuccessOnRetryAndSerializedContext() throws Throwable {
    MockRetryCallback callback = new MockRetryCallback();

    RetryState retryState = new DefaultRetryState("foo");

    RetryTemplate retryTemplate = new RetryTemplate();
    RetryContextCache cache = new SerializedMapRetryContextCache();
    retryTemplate.setRetryContextCache(cache);
    retryTemplate.setRetryPolicy(new SimpleRetryPolicy(2));

    assertFalse(cache.containsKey("foo"));

    Object result = "start_foo";
    try {
      result = retryTemplate.execute(callback, retryState);
      // The first failed attempt we expect to retry...
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
      assertNull(e.getMessage());
    }

    assertTrue(cache.containsKey("foo"));

    result = retryTemplate.execute(callback, retryState);

    assertFalse(cache.containsKey("foo"));

    assertEquals(2, callback.attempts);
    assertEquals(1, callback.context.getRetryCount());
    assertEquals("bar", result);
  }

  @Test
  public void testExponentialBackOffIsExponential() throws Throwable {
    ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
    policy.setInitialInterval(100);
    policy.setMultiplier(1.5);
    RetryTemplate template = new RetryTemplate();
    template.setBackOffPolicy(policy);
    final List<Long> times = new ArrayList<Long>();
    RetryState retryState = new DefaultRetryState("bar");
    for (int i = 0; i < 3; i++) {
      try {
        template.execute(new RetryCallback<String, Exception>() {
          public String doWithRetry(RetryContext context) throws Exception {
            times.add(System.currentTimeMillis());
            throw new Exception("Fail");
          }
        }, new RecoveryCallback<String>() {
          public String recover(RetryContext context) throws Exception {
            return null;
          }
        }, retryState);
      }
      catch (Exception e) {
        assertTrue(e.getMessage().equals("Fail"));
      }
    }
    assertEquals(3, times.size());
    assertTrue(times.get(1) - times.get(0) >= 100);
    assertTrue(times.get(2) - times.get(1) >= 150);
  }

  @Test
  public void testExternalRetryWithFailAndNoRetryWhenKeyIsNull() throws Throwable {
    MockRetryCallback callback = new MockRetryCallback();

    RetryState retryState = new DefaultRetryState(null);

    RetryTemplate retryTemplate = new RetryTemplate();
    MapRetryContextCache cache = new MapRetryContextCache();
    retryTemplate.setRetryContextCache(cache);
    retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1));

    try {
      retryTemplate.execute(callback, retryState);
      // The first failed attempt...
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
      assertEquals(null, e.getMessage());
    }

    retryTemplate.execute(callback, retryState);
    // The second attempt is successful by design...

    // Callback is called twice because its state is null: the recovery path should
    // not be called...
    assertEquals(2, callback.attempts);
  }

  /**
   * @author Dave Syer
   */
  private static final class MockRetryCallback implements RetryCallback<String, Exception> {

    int attempts = 0;

    RetryContext context;

    public String doWithRetry(RetryContext context) throws Exception {
      attempts++;
      this.context = context;
      if (attempts < 2) {
        throw new RuntimeException();
      }
      return "bar";
    }

  }

}
