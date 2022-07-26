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

import org.junit.jupiter.api.Test;

import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryListener;
import cn.taketoday.retry.RetryState;
import cn.taketoday.retry.RetryStatistics;
import cn.taketoday.retry.policy.SimpleRetryPolicy;
import cn.taketoday.retry.support.DefaultRetryState;
import cn.taketoday.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 */
public class StatisticsListenerTests {

  private final StatisticsRepository repository = new DefaultStatisticsRepository();

  private final StatisticsListener listener = new StatisticsListener(repository);

  @Test
  public void testStatelessSuccessful() throws Throwable {
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setListeners(new RetryListener[] { listener });
    for (int x = 1; x <= 10; x++) {
      MockRetryCallback callback = new MockRetryCallback();
      callback.setAttemptsBeforeSuccess(x);
      retryTemplate.setRetryPolicy(new SimpleRetryPolicy(x));
      retryTemplate.execute(callback);
      assertThat(callback.attempts).isEqualTo(x);
      RetryStatistics stats = repository.findOne("test");
      // System.err.println(stats);
      assertThat(stats).isNotNull();
      assertThat(stats.getCompleteCount()).isEqualTo(x);
      assertThat(stats.getStartedCount()).isEqualTo((x + 1) * x / 2);
      assertThat(stats.getErrorCount() + x).isEqualTo(stats.getStartedCount());
    }
  }

  @Test
  public void testStatefulSuccessful() {
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setListeners(new RetryListener[] { listener });
    RetryState state = new DefaultRetryState("foo");
    for (int x = 1; x <= 10; x++) {
      MockRetryCallback callback = new MockRetryCallback();
      callback.setAttemptsBeforeSuccess(x);
      retryTemplate.setRetryPolicy(new SimpleRetryPolicy(x));
      for (int i = 0; i < x; i++) {
        try {
          retryTemplate.execute(callback, state);
        }
        catch (Exception e) {
          // don't care
        }
      }
      assertThat(callback.attempts).isEqualTo(x);
      RetryStatistics stats = repository.findOne("test");
      // System.err.println(stats);
      assertThat(stats).isNotNull();
      assertThat(stats.getCompleteCount()).isEqualTo(x);
      assertThat(stats.getStartedCount()).isEqualTo((x + 1) * x / 2);
      assertThat(stats.getErrorCount() + x).isEqualTo(stats.getStartedCount());
    }
  }

  @Test
  public void testStatelessUnsuccessful() {
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setListeners(new RetryListener[] { listener });
    for (int x = 1; x <= 10; x++) {
      MockRetryCallback callback = new MockRetryCallback();
      callback.setAttemptsBeforeSuccess(x + 1);
      retryTemplate.setRetryPolicy(new SimpleRetryPolicy(x));
      try {
        retryTemplate.execute(callback);
      }
      catch (Exception e) {
        // not interested
      }
      assertThat(callback.attempts).isEqualTo(x);
      RetryStatistics stats = repository.findOne("test");
      assertThat(stats).isNotNull();
      assertThat(stats.getAbortCount()).isEqualTo(x);
      assertThat(stats.getStartedCount()).isEqualTo((x + 1) * x / 2);
      assertThat(stats.getErrorCount()).isEqualTo(stats.getStartedCount());
    }
  }

  @Test
  public void testStatefulUnsuccessful() {
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setListeners(new RetryListener[] { listener });
    RetryState state = new DefaultRetryState("foo");
    for (int x = 1; x <= 10; x++) {
      MockRetryCallback callback = new MockRetryCallback();
      callback.setAttemptsBeforeSuccess(x + 1);
      retryTemplate.setRetryPolicy(new SimpleRetryPolicy(x));
      for (int i = 0; i < x + 1; i++) {
        try {
          retryTemplate.execute(callback, state);
        }
        catch (Exception e) {
          // don't care
        }
      }
      assertThat(callback.attempts).isEqualTo(x);
      RetryStatistics stats = repository.findOne("test");
      // System.err.println(stats);
      assertThat(stats).isNotNull();
      assertThat(stats.getAbortCount()).isEqualTo(x);
      assertThat(stats.getStartedCount()).isEqualTo((x + 1) * x / 2);
      assertThat(stats.getErrorCount()).isEqualTo(stats.getStartedCount());
    }
  }

  @Test
  public void testStatelessRecovery() throws Throwable {
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setListeners(new RetryListener[] { listener });
    for (int x = 1; x <= 10; x++) {
      MockRetryCallback callback = new MockRetryCallback();
      callback.setAttemptsBeforeSuccess(x + 1);
      retryTemplate.setRetryPolicy(new SimpleRetryPolicy(x));
      retryTemplate.execute(callback, context -> null);
      assertThat(callback.attempts).isEqualTo(x);
      RetryStatistics stats = repository.findOne("test");
      // System.err.println(stats);
      assertThat(stats).isNotNull();
      assertThat(stats.getRecoveryCount()).isEqualTo(x);
      assertThat(stats.getStartedCount()).isEqualTo((x + 1) * x / 2);
      assertThat(stats.getErrorCount()).isEqualTo(stats.getStartedCount());
    }
  }

  @Test
  public void testStatefulRecovery() {
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setListeners(new RetryListener[] { listener });
    RetryState state = new DefaultRetryState("foo");
    for (int x = 1; x <= 10; x++) {
      MockRetryCallback callback = new MockRetryCallback();
      callback.setAttemptsBeforeSuccess(x + 1);
      retryTemplate.setRetryPolicy(new SimpleRetryPolicy(x));
      for (int i = 0; i < x + 1; i++) {
        try {
          retryTemplate.execute(callback, context -> null, state);
        }
        catch (Exception e) {
          // don't care
        }
      }
      assertThat(callback.attempts).isEqualTo(x);
      RetryStatistics stats = repository.findOne("test");
      // System.err.println(stats);
      assertThat(stats).isNotNull();
      assertThat(stats.getRecoveryCount()).isEqualTo(x);
      assertThat(stats.getStartedCount()).isEqualTo((x + 1) * x / 2);
      assertThat(stats.getErrorCount()).isEqualTo(stats.getStartedCount());
    }
  }

  private static class MockRetryCallback implements RetryCallback<Object, Exception> {

    private int attempts;

    private int attemptsBeforeSuccess;

    private final Exception exceptionToThrow = new Exception();

    @Override
    public Object doWithRetry(RetryContext status) throws Exception {
      status.setAttribute(RetryContext.NAME, "test");
      this.attempts++;
      if (this.attempts < this.attemptsBeforeSuccess) {
        throw this.exceptionToThrow;
      }
      return null;
    }

    public void setAttemptsBeforeSuccess(int attemptsBeforeSuccess) {
      this.attemptsBeforeSuccess = attemptsBeforeSuccess;
    }

  }

}
