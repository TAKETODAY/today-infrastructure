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

package cn.taketoday.retry.stats;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import cn.taketoday.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Gary Russell
 */
public class ExponentialAverageRetryStatisticsTests {

  private final ExponentialAverageRetryStatistics stats = new ExponentialAverageRetryStatistics("test");

  @Test
  public void pointless() {
    stats.setName("spam");
    assertThat(stats.getName()).isEqualTo("spam");
  }

  @Test
  public void attributes() {
    stats.setAttribute("foo", "bar");
    assertThat(stats.getAttribute("foo")).isEqualTo("bar");
    assertThat(Arrays.asList(stats.getAttributeNames()).contains("foo")).isTrue();
  }

  @Test
  public void abortCount() {
    stats.incrementAbortCount();
    assertThat(stats.getAbortCount()).isEqualTo(1);
    // rounds up to 1
    assertThat(stats.getRollingAbortCount()).isEqualTo(1);
  }

  @Test
  public void errorCount() {
    stats.incrementErrorCount();
    assertThat(stats.getErrorCount()).isEqualTo(1);
    // rounds up to 1
    assertThat(stats.getRollingErrorCount()).isEqualTo(1);
  }

  @Test
  public void startedCount() {
    stats.incrementStartedCount();
    assertThat(stats.getStartedCount()).isEqualTo(1);
    // rounds up to 1
    assertThat(stats.getRollingStartedCount()).isEqualTo(1);
  }

  @Test
  public void completeCount() {
    stats.incrementCompleteCount();
    assertThat(stats.getCompleteCount()).isEqualTo(1);
    // rounds up to 1
    assertThat(stats.getRollingCompleteCount()).isEqualTo(1);
  }

  @Test
  public void recoveryCount() {
    stats.incrementRecoveryCount();
    assertThat(stats.getRecoveryCount()).isEqualTo(1);
    // rounds up to 1
    assertThat(stats.getRollingRecoveryCount()).isEqualTo(1);
  }

  @Test
  public void oldValuesDecay() {
    stats.incrementAbortCount();
    assertThat(stats.getAbortCount()).isEqualTo(1);
    // Wind back time to epoch 0
    Object abort = ReflectionTestUtils.getField(stats, "abort");
    ReflectionTestUtils.setField(abort, "lastTime", 0);
    // rounds down to 1
    assertThat(stats.getRollingAbortCount()).isEqualTo(0);
  }

}
