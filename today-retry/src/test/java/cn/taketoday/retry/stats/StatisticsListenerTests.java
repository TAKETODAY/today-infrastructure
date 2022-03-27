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

import org.junit.Test;

import cn.taketoday.retry.RecoveryCallback;
import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryListener;
import cn.taketoday.retry.RetryState;
import cn.taketoday.retry.RetryStatistics;
import cn.taketoday.retry.policy.SimpleRetryPolicy;
import cn.taketoday.retry.support.DefaultRetryState;
import cn.taketoday.retry.support.RetryTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Dave Syer
 */
public class StatisticsListenerTests {

  private StatisticsRepository repository = new DefaultStatisticsRepository();

  private StatisticsListener listener = new StatisticsListener(repository);

  @Test
  public void testStatelessSuccessful() throws Throwable {
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setListeners(new RetryListener[] { listener });
    for (int x = 1; x <= 10; x++) {
      MockRetryCallback callback = new MockRetryCallback();
      callback.setAttemptsBeforeSuccess(x);
      retryTemplate.setRetryPolicy(new SimpleRetryPolicy(x));
      retryTemplate.execute(callback);
      assertEquals(x, callback.attempts);
      RetryStatistics stats = repository.findOne("test");
      // System.err.println(stats);
      assertNotNull(stats);
      assertEquals(x, stats.getCompleteCount());
      assertEquals((x + 1) * x / 2, stats.getStartedCount());
      assertEquals(stats.getStartedCount(), stats.getErrorCount() + x);
    }
  }

  @Test
  public void testStatefulSuccessful() throws Throwable {
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
      assertEquals(x, callback.attempts);
      RetryStatistics stats = repository.findOne("test");
      // System.err.println(stats);
      assertNotNull(stats);
      assertEquals(x, stats.getCompleteCount());
      assertEquals((x + 1) * x / 2, stats.getStartedCount());
      assertEquals(stats.getStartedCount(), stats.getErrorCount() + x);
    }
  }

  @Test
  public void testStatelessUnsuccessful() throws Throwable {
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
      assertEquals(x, callback.attempts);
      RetryStatistics stats = repository.findOne("test");
      assertNotNull(stats);
      assertEquals(x, stats.getAbortCount());
      assertEquals((x + 1) * x / 2, stats.getStartedCount());
      assertEquals(stats.getStartedCount(), stats.getErrorCount());
    }
  }

  @Test
  public void testStatefulUnsuccessful() throws Throwable {
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
      assertEquals(x, callback.attempts);
      RetryStatistics stats = repository.findOne("test");
      // System.err.println(stats);
      assertNotNull(stats);
      assertEquals(x, stats.getAbortCount());
      assertEquals((x + 1) * x / 2, stats.getStartedCount());
      assertEquals(stats.getStartedCount(), stats.getErrorCount());
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
      retryTemplate.execute(callback, new RecoveryCallback<Object>() {
        @Override
        public Object recover(RetryContext context) throws Exception {
          return null;
        }
      });
      assertEquals(x, callback.attempts);
      RetryStatistics stats = repository.findOne("test");
      // System.err.println(stats);
      assertNotNull(stats);
      assertEquals(x, stats.getRecoveryCount());
      assertEquals((x + 1) * x / 2, stats.getStartedCount());
      assertEquals(stats.getStartedCount(), stats.getErrorCount());
    }
  }

  @Test
  public void testStatefulRecovery() throws Throwable {
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setListeners(new RetryListener[] { listener });
    RetryState state = new DefaultRetryState("foo");
    for (int x = 1; x <= 10; x++) {
      MockRetryCallback callback = new MockRetryCallback();
      callback.setAttemptsBeforeSuccess(x + 1);
      retryTemplate.setRetryPolicy(new SimpleRetryPolicy(x));
      for (int i = 0; i < x + 1; i++) {
        try {
          retryTemplate.execute(callback, new RecoveryCallback<Object>() {
            @Override
            public Object recover(RetryContext context) throws Exception {
              return null;
            }
          }, state);
        }
        catch (Exception e) {
          // don't care
        }
      }
      assertEquals(x, callback.attempts);
      RetryStatistics stats = repository.findOne("test");
      // System.err.println(stats);
      assertNotNull(stats);
      assertEquals(x, stats.getRecoveryCount());
      assertEquals((x + 1) * x / 2, stats.getStartedCount());
      assertEquals(stats.getStartedCount(), stats.getErrorCount());
    }
  }

  private static class MockRetryCallback implements RetryCallback<Object, Exception> {

    private int attempts;

    private int attemptsBeforeSuccess;

    private Exception exceptionToThrow = new Exception();

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
