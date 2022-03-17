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

package cn.taketoday.test.web.client;

import cn.taketoday.lang.Assert;

/**
 * A simple type representing a range for an expected count.
 *
 * <p>Examples:
 * <pre>
 * import static cn.taketoday.test.web.client.ExpectedCount.*
 *
 * once()
 * twice()
 * manyTimes()
 * times(5)
 * min(2)
 * max(4)
 * between(2, 4)
 * never()
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class ExpectedCount {

  private final int minCount;

  private final int maxCount;

  /**
   * Private constructor.
   * See static factory methods in this class.
   */
  private ExpectedCount(int minCount, int maxCount) {
    Assert.isTrue(minCount >= 0, "minCount >= 0 is required");
    Assert.isTrue(maxCount >= minCount, "maxCount >= minCount is required");
    this.minCount = minCount;
    this.maxCount = maxCount;
  }

  /**
   * Return the {@code min} boundary of the expected count range.
   */
  public int getMinCount() {
    return this.minCount;
  }

  /**
   * Return the {@code max} boundary of the expected count range.
   */
  public int getMaxCount() {
    return this.maxCount;
  }

  /**
   * Exactly once.
   */
  public static ExpectedCount once() {
    return new ExpectedCount(1, 1);
  }

  /**
   * Exactly twice.
   */
  public static ExpectedCount twice() {
    return new ExpectedCount(2, 2);
  }

  /**
   * Many times (range of 1..Integer.MAX_VALUE).
   */
  public static ExpectedCount manyTimes() {
    return new ExpectedCount(1, Integer.MAX_VALUE);
  }

  /**
   * Exactly N times.
   */
  public static ExpectedCount times(int count) {
    Assert.isTrue(count >= 1, "'count' must be >= 1");
    return new ExpectedCount(count, count);
  }

  /**
   * At least {@code min} number of times.
   */
  public static ExpectedCount min(int min) {
    Assert.isTrue(min >= 1, "'min' must be >= 1");
    return new ExpectedCount(min, Integer.MAX_VALUE);
  }

  /**
   * At most {@code max} number of times.
   */
  public static ExpectedCount max(int max) {
    Assert.isTrue(max >= 1, "'max' must be >= 1");
    return new ExpectedCount(1, max);
  }

  /**
   * No calls expected at all, i.e. min=0 and max=0.
   *
   * @since 4.0
   */
  public static ExpectedCount never() {
    return new ExpectedCount(0, 0);
  }

  /**
   * Between {@code min} and {@code max} number of times.
   */
  public static ExpectedCount between(int min, int max) {
    return new ExpectedCount(min, max);
  }

}
