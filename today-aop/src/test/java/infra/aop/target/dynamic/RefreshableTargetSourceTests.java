/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.aop.target.dynamic;

import org.junit.jupiter.api.Test;

import infra.aop.target.AbstractRefreshableTargetSource;
import infra.core.testfixture.DisabledIfInContinuousIntegration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
public class RefreshableTargetSourceTests {

  /**
   * Test what happens when checking for refresh but not refreshing object.
   */
  @Test
  public void testRefreshCheckWithNonRefresh() throws Exception {
    CountingRefreshableTargetSource ts = new CountingRefreshableTargetSource();
    ts.setRefreshCheckDelay(0);

    Object a = ts.getTarget();
    Thread.sleep(1);
    Object b = ts.getTarget();

    assertThat(ts.getCallCount()).as("Should be one call to freshTarget to get initial target").isEqualTo(1);
    assertThat(b).as("Returned objects should be the same - no refresh should occur").isSameAs(a);
  }

  /**
   * Test what happens when checking for refresh and refresh occurs.
   */
  @Test
  public void testRefreshCheckWithRefresh() throws Exception {
    CountingRefreshableTargetSource ts = new CountingRefreshableTargetSource(true);
    ts.setRefreshCheckDelay(0);

    Object a = ts.getTarget();
    Thread.sleep(100);
    Object b = ts.getTarget();

    assertThat(ts.getCallCount()).as("Should have called freshTarget twice").isEqualTo(2);
    assertThat(b).as("Should be different objects").isNotSameAs(a);
  }

  /**
   * Test what happens when no refresh occurs.
   */
  @Test
  public void testWithNoRefreshCheck() throws Exception {
    CountingRefreshableTargetSource ts = new CountingRefreshableTargetSource(true);
    ts.setRefreshCheckDelay(-1);

    Object a = ts.getTarget();
    Object b = ts.getTarget();

    assertThat(ts.getCallCount()).as("Refresh target should only be called once").isEqualTo(1);
    assertThat(b).as("Objects should be the same - refresh check delay not elapsed").isSameAs(a);
  }

  @Test
  @DisabledIfInContinuousIntegration
  public void testRefreshOverTime() throws Exception {
    CountingRefreshableTargetSource ts = new CountingRefreshableTargetSource(true);
    ts.setRefreshCheckDelay(100);

    Object a = ts.getTarget();
    Object b = ts.getTarget();
    assertThat(b).as("Objects should be same").isEqualTo(a);

    Thread.sleep(50);

    Object c = ts.getTarget();
    assertThat(c).as("A and C should be same").isEqualTo(a);

    Thread.sleep(60);

    Object d = ts.getTarget();
    assertThat(d).as("D should not be null").isNotNull();
    assertThat(a.equals(d)).as("A and D should not be equal").isFalse();

    Object e = ts.getTarget();
    assertThat(e).as("D and E should be equal").isEqualTo(d);

    Thread.sleep(110);

    Object f = ts.getTarget();
    assertThat(e.equals(f)).as("E and F should be different").isFalse();
  }

  private static class CountingRefreshableTargetSource extends AbstractRefreshableTargetSource {

    private int callCount;

    private boolean requiresRefresh;

    public CountingRefreshableTargetSource() {
    }

    public CountingRefreshableTargetSource(boolean requiresRefresh) {
      this.requiresRefresh = requiresRefresh;
    }

    @Override
    protected Object freshTarget() {
      this.callCount++;
      return new Object();
    }

    public int getCallCount() {
      return this.callCount;
    }

    @Override
    protected boolean requiresRefresh() {
      return this.requiresRefresh;
    }
  }

}
