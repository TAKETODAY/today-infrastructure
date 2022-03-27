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

import java.util.Arrays;

import cn.taketoday.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
public class ExponentialAverageRetryStatisticsTests {

  private ExponentialAverageRetryStatistics stats = new ExponentialAverageRetryStatistics("test");

  @Test
  public void pointless() throws Exception {
    stats.setName("spam");
    assertEquals("spam", stats.getName());
    assertNotNull(stats.toString());
  }

  @Test
  public void attributes() throws Exception {
    stats.setAttribute("foo", "bar");
    assertEquals("bar", stats.getAttribute("foo"));
    assertTrue(Arrays.asList(stats.getAttributeNames()).contains("foo"));
  }

  @Test
  public void abortCount() throws Exception {
    stats.incrementAbortCount();
    assertEquals(1, stats.getAbortCount());
    // rounds up to 1
    assertEquals(1, stats.getRollingAbortCount());
  }

  @Test
  public void errorCount() throws Exception {
    stats.incrementErrorCount();
    assertEquals(1, stats.getErrorCount());
    // rounds up to 1
    assertEquals(1, stats.getRollingErrorCount());
  }

  @Test
  public void startedCount() throws Exception {
    stats.incrementStartedCount();
    assertEquals(1, stats.getStartedCount());
    // rounds up to 1
    assertEquals(1, stats.getRollingStartedCount());
  }

  @Test
  public void completeCount() throws Exception {
    stats.incrementCompleteCount();
    assertEquals(1, stats.getCompleteCount());
    // rounds up to 1
    assertEquals(1, stats.getRollingCompleteCount());
  }

  @Test
  public void recoveryCount() throws Exception {
    stats.incrementRecoveryCount();
    assertEquals(1, stats.getRecoveryCount());
    // rounds up to 1
    assertEquals(1, stats.getRollingRecoveryCount());
  }

  @Test
  public void oldValuesDecay() throws Exception {
    stats.incrementAbortCount();
    assertEquals(1, stats.getAbortCount());
    // Wind back time to epoch 0
    ReflectionTestUtils.setField(ReflectionTestUtils.getField(stats, "abort"), "lastTime", 0);
    // rounds down to 1
    assertEquals(0, stats.getRollingAbortCount());
  }

}
