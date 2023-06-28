/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 * @author Gary Russell
 */
public class CircuitBreakerStatisticsTests {

  private static final String RECOVERED = "RECOVERED";

  private static final String RESULT = "RESULT";

  private RetryTemplate retryTemplate;

  private RecoveryCallback<Object> recovery;

  private MockRetryCallback callback;

  private DefaultRetryState state;

  private final StatisticsRepository repository = new DefaultStatisticsRepository();

  private final StatisticsListener listener = new StatisticsListener(repository);

  private RetryContextCache cache;

  @BeforeEach
  public void init() {
    this.callback = new MockRetryCallback();
    this.recovery = context -> RECOVERED;
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
    assertThat(stats.getStartedCount()).isEqualTo(1);
    assertThat(result).isEqualTo(RECOVERED);
    result = this.retryTemplate.execute(this.callback, this.recovery, this.state);
    assertThat(result).isEqualTo(RECOVERED);
    assertThat(stats.getRecoveryCount()).describedAs("There should be two recoveries").isEqualTo(2);
    assertThat(stats.getErrorCount())
            .describedAs("There should only be one error because the circuit is now open").isEqualTo(1);
    assertThat(stats.getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_OPEN)).isEqualTo(Boolean.TRUE);
    // Both recoveries are through a short circuit because we used NeverRetryPolicy
    assertThat(stats.getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_SHORT_COUNT)).isEqualTo(2);
    resetAndAssert(this.cache, stats);
  }

  @Test
  public void testFailedRecoveryCountsAsAbort() throws Throwable {
    this.retryTemplate.setRetryPolicy(new CircuitBreakerRetryPolicy(new NeverRetryPolicy()));
    this.recovery = context -> {
      throw new ExhaustedRetryException("Planned exhausted");
    };
    assertThatExceptionOfType(ExhaustedRetryException.class)
            .isThrownBy(() -> this.retryTemplate.execute(this.callback, this.recovery, this.state));
    MutableRetryStatistics stats = (MutableRetryStatistics) repository.findOne("test");
    assertThat(stats.getStartedCount()).isEqualTo(1);
    assertThat(stats.getAbortCount()).isEqualTo(1);
    assertThat(stats.getRecoveryCount()).isEqualTo(0);
  }

  @Test
  public void testCircuitOpenWithNoRecovery() {
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
    assertThat(stats.getAbortCount()).describedAs("There should be two aborts").isEqualTo(2);
    assertThat(stats.getErrorCount())
            .describedAs("There should only be one error because the circuit is now open").isEqualTo(1);
    assertThat(stats.getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_OPEN)).isEqualTo(true);
    resetAndAssert(this.cache, stats);
  }

  private void resetAndAssert(RetryContextCache cache, MutableRetryStatistics stats) {
    reset(cache.get("retry"));
    listener.close(cache.get("retry"), callback, null);
    assertThat(stats.getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_SHORT_COUNT)).isEqualTo(0);
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
