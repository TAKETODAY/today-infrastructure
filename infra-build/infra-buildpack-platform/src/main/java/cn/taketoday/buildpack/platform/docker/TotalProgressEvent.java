/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.docker;

import cn.taketoday.lang.Assert;

/**
 * Event published by the {@link TotalProgressPullListener} showing the total progress of
 * an operation.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class TotalProgressEvent {

  private final int percent;

  /**
   * Create a new {@link TotalProgressEvent} with a specific percent value.
   *
   * @param percent the progress as a percentage
   */
  public TotalProgressEvent(int percent) {
    Assert.isTrue(percent >= 0 && percent <= 100, "Percent must be in the range 0 to 100");
    this.percent = percent;
  }

  /**
   * Return the total progress.
   *
   * @return the total progress
   */
  public int getPercent() {
    return this.percent;
  }

}
