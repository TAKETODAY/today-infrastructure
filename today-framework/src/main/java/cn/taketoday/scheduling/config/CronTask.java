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

import cn.taketoday.scheduling.support.CronTrigger;

/**
 * {@link TriggerTask} implementation defining a {@code Runnable} to be executed according
 * to a {@linkplain cn.taketoday.scheduling.support.CronExpression#parse(String)
 * standard cron expression}.
 *
 * @author Chris Beams
 * @see cn.taketoday.scheduling.annotation.Scheduled#cron()
 * @see ScheduledTaskRegistrar#addCronTask(CronTask)
 * @since 4.0
 */
public class CronTask extends TriggerTask {

  private final String expression;

  /**
   * Create a new {@code CronTask}.
   *
   * @param runnable the underlying task to execute
   * @param expression the cron expression defining when the task should be executed
   */
  public CronTask(Runnable runnable, String expression) {
    this(runnable, new CronTrigger(expression));
  }

  /**
   * Create a new {@code CronTask}.
   *
   * @param runnable the underlying task to execute
   * @param cronTrigger the cron trigger defining when the task should be executed
   */
  public CronTask(Runnable runnable, CronTrigger cronTrigger) {
    super(runnable, cronTrigger);
    this.expression = cronTrigger.getExpression();
  }

  /**
   * Return the cron expression defining when the task should be executed.
   */
  public String getExpression() {
    return this.expression;
  }

}
