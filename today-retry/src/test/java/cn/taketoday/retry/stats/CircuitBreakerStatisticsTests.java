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

package cn.taketoday.retry.stats;

import org.junit.Before;
import org.junit.Test;

import cn.taketoday.classify.BinaryExceptionClassifier;
import cn.taketoday.retry.ExhaustedRetryException;
import cn.taketoday.retry.RecoveryCallback;
import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryListener;
import cn.taketoday.retry.policy.CircuitBreakerRetryPolicy;
import cn.taketoday.retry.policy.MapRetryContextCache;
import cn.taketoday.retry.policy.NeverRetryPolicy;
import cn.taketoday.retry.policy.RetryContextCache;
import cn.taketoday.retry.support.DefaultRetryState;
import cn.taketoday.retry.support.RetryTemplate;
import cn.taketoday.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Dave Syer
 */
public class CircuitBreakerStatisticsTests {

  private static final String RECOVERED = "RECOVERED";

  private static final String RESULT = "RESULT";

  private RetryTemplate retryTemplate;

  private RecoveryCallback<Object> recovery;

  private MockRetryCallback callback;

  private DefaultRetryState state;

  private StatisticsRepository repository = new DefaultStatisticsRepository();

  private StatisticsListener listener = new StatisticsListener(repository);

  private RetryContextCache cache;

  @Before
  public void init() {
    this.callback = new MockRetryCallback();
    this.recovery = new RecoveryCallback<Object>() {
      @Override
      public Object recover(RetryContext context) throws Exception {
        return RECOVERED;
      }
    };
    this.retryTemplate = new RetryTemplate();
    this.cache = new MapRetryContextCache();
    this.retryTemplate.setRetryContextCache(this.cache);
    retryTemplate.setListeners(new RetryListener[] { listener });
    this.callback.setAttemptsBeforeSuccess(1);
    // No rollback by default (so exceptions are not rethrown)
    this.state = new DefaultRetryState("retry", new BinaryExceptionClassifier(false));
  }

  @Test
  public void testCircuitOpenWhenNotRetryable() throws Throwable {
    this.retryTemplate.setRetryPolicy(new CircuitBreakerRetryPolicy(new NeverRetryPolicy()));
    Object result = this.retryTemplate.execute(this.callback, this.recovery, this.state);
    MutableRetryStatistics stats = (MutableRetryStatistics) repository.findOne("test");
    assertEquals(1, stats.getStartedCount());
    assertEquals(RECOVERED, result);
    result = this.retryTemplate.execute(this.callback, this.recovery, this.state);
    assertEquals(RECOVERED, result);
    assertEquals("There should be two recoveries", 2, stats.getRecoveryCount());
    assertEquals("There should only be one error because the circuit is now open", 1, stats.getErrorCount());
    assertEquals(true, stats.getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_OPEN));
    // Both recoveries are through a short circuit because we used NeverRetryPolicy
    assertEquals(2, stats.getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_SHORT_COUNT));
    resetAndAssert(this.cache, stats);
  }

  @Test
  public void testFailedRecoveryCountsAsAbort() throws Throwable {
    this.retryTemplate.setRetryPolicy(new CircuitBreakerRetryPolicy(new NeverRetryPolicy()));
    this.recovery = new RecoveryCallback<Object>() {
      @Override
      public Object recover(RetryContext context) throws Exception {
        throw new ExhaustedRetryException("Planned exhausted");
      }
    };
    try {
      this.retryTemplate.execute(this.callback, this.recovery, this.state);
      fail("Expected ExhaustedRetryException");
    }
    catch (ExhaustedRetryException e) {
      // Fine
    }
    MutableRetryStatistics stats = (MutableRetryStatistics) repository.findOne("test");
    assertEquals(1, stats.getStartedCount());
    assertEquals(1, stats.getAbortCount());
    assertEquals(0, stats.getRecoveryCount());
  }

  @Test
  public void testCircuitOpenWithNoRecovery() throws Throwable {
    this.retryTemplate.setRetryPolicy(new CircuitBreakerRetryPolicy(new NeverRetryPolicy()));
    this.retryTemplate.setThrowLastExceptionOnExhausted(true);
    try {
      this.retryTemplate.execute(this.callback, this.state);
    }
    catch (Exception e) {
    }
    try {
      this.retryTemplate.execute(this.callback, this.state);
    }
    catch (Exception e) {
    }
    MutableRetryStatistics stats = (MutableRetryStatistics) repository.findOne("test");
    assertEquals("There should be two aborts", 2, stats.getAbortCount());
    assertEquals("There should only be one error because the circuit is now open", 1, stats.getErrorCount());
    assertEquals(true, stats.getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_OPEN));
    resetAndAssert(this.cache, stats);
  }

  private void resetAndAssert(RetryContextCache cache, MutableRetryStatistics stats) {
    reset(cache.get("retry"));
    listener.close(cache.get("retry"), callback, null);
    assertEquals(0, stats.getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_SHORT_COUNT));
  }

  private void reset(RetryContext retryContext) {
    ReflectionTestUtils.invokeMethod(retryContext, "reset");
  }

  protected static class MockRetryCallback implements RetryCallback<Object, Exception> {

    private int attemptsBeforeSuccess;

    private Exception exceptionToThrow = new Exception();

    private RetryContext status;

    @Override
    public Object doWithRetry(RetryContext status) throws Exception {
      status.setAttribute(RetryContext.NAME, "test");
      this.status = status;
      int attempts = (Integer) status.getAttribute("attempts");
      attempts++;
      status.setAttribute("attempts", attempts);
      if (attempts <= this.attemptsBeforeSuccess) {
        throw this.exceptionToThrow;
      }
      return RESULT;
    }

    public boolean isOpen() {
      return status != null && status.getAttribute("open") == Boolean.TRUE;
    }

    public void setAttemptsBeforeSuccess(int attemptsBeforeSuccess) {
      this.attemptsBeforeSuccess = attemptsBeforeSuccess;
    }

    public void setExceptionToThrow(Exception exceptionToThrow) {
      this.exceptionToThrow = exceptionToThrow;
    }

  }

}
