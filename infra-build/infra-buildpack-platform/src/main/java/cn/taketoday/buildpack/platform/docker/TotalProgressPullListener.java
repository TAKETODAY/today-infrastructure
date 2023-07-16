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

import java.util.function.Consumer;

/**
 * {@link UpdateListener} that calculates the total progress of the entire pull operation
 * and publishes {@link TotalProgressEvent}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 4.0
 */
public class TotalProgressPullListener extends TotalProgressListener<PullImageUpdateEvent> {

  private static final String[] TRACKED_STATUS_KEYS = { "Downloading", "Extracting" };

  /**
   * Create a new {@link TotalProgressPullListener} that prints a progress bar to
   * {@link System#out}.
   *
   * @param prefix the prefix to output
   */
  public TotalProgressPullListener(String prefix) {
    this(new TotalProgressBar(prefix));
  }

  /**
   * Create a new {@link TotalProgressPullListener} that sends {@link TotalProgressEvent
   * events} to the given consumer.
   *
   * @param consumer the consumer that receives {@link TotalProgressEvent progress
   * events}
   */
  public TotalProgressPullListener(Consumer<TotalProgressEvent> consumer) {
    super(consumer, TRACKED_STATUS_KEYS);
  }

}
