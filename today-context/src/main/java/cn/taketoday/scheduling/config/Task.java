/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.scheduling.config;

import cn.taketoday.lang.Assert;

/**
 * Holder class defining a {@code Runnable} to be executed as a task, typically at a
 * scheduled time or interval. See subclass hierarchy for various scheduling approaches.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 4.0
 */
public class Task {

  private final Runnable runnable;

  /**
   * Create a new {@code Task}.
   *
   * @param runnable the underlying task to execute
   */
  public Task(Runnable runnable) {
    Assert.notNull(runnable, "Runnable is required");
    this.runnable = runnable;
  }

  /**
   * Return the underlying task.
   */
  public Runnable getRunnable() {
    return this.runnable;
  }

  @Override
  public String toString() {
    return this.runnable.toString();
  }

}
