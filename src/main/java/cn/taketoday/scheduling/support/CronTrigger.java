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

package cn.taketoday.scheduling.support;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scheduling.Trigger;
import cn.taketoday.scheduling.TriggerContext;

/**
 * {@link Trigger} implementation for cron expressions.
 * Wraps a {@link CronExpression}.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @see CronExpression
 * @since 4.0
 */
public class CronTrigger implements Trigger {

  private final CronExpression expression;

  private final ZoneId zoneId;

  /**
   * Build a {@code CronTrigger} from the pattern provided in the default time zone.
   *
   * @param expression a space-separated list of time fields, following cron
   * expression conventions
   */
  public CronTrigger(String expression) {
    this(expression, ZoneId.systemDefault());
  }

  /**
   * Build a {@code CronTrigger} from the pattern provided in the given time zone.
   *
   * @param expression a space-separated list of time fields, following cron
   * expression conventions
   * @param timeZone a time zone in which the trigger times will be generated
   */
  public CronTrigger(String expression, TimeZone timeZone) {
    this(expression, timeZone.toZoneId());
  }

  /**
   * Build a {@code CronTrigger} from the pattern provided in the given time zone.
   *
   * @param expression a space-separated list of time fields, following cron
   * expression conventions
   * @param zoneId a time zone in which the trigger times will be generated
   * @see CronExpression#parse(String)
   * @since 4.0
   */
  public CronTrigger(String expression, ZoneId zoneId) {
    Assert.hasLength(expression, "Expression must not be empty");
    Assert.notNull(zoneId, "ZoneId must not be null");

    this.expression = CronExpression.parse(expression);
    this.zoneId = zoneId;
  }

  /**
   * Return the cron pattern that this trigger has been built with.
   */
  public String getExpression() {
    return this.expression.toString();
  }

  /**
   * Determine the next execution time according to the given trigger context.
   * <p>Next execution times are calculated based on the
   * {@linkplain TriggerContext#lastCompletionTime completion time} of the
   * previous execution; therefore, overlapping executions won't occur.
   */
  @Override
  public Date nextExecutionTime(TriggerContext triggerContext) {
    Date date = triggerContext.lastCompletionTime();
    if (date != null) {
      Date scheduled = triggerContext.lastScheduledExecutionTime();
      if (scheduled != null && date.before(scheduled)) {
        // Previous task apparently executed too early...
        // Let's simply use the last calculated execution time then,
        // in order to prevent accidental re-fires in the same second.
        date = scheduled;
      }
    }
    else {
      date = new Date(triggerContext.getClock().millis());
    }
    ZonedDateTime dateTime = ZonedDateTime.ofInstant(date.toInstant(), this.zoneId);
    ZonedDateTime next = this.expression.next(dateTime);
    return (next != null ? Date.from(next.toInstant()) : null);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof CronTrigger &&
            this.expression.equals(((CronTrigger) other).expression)));
  }

  @Override
  public int hashCode() {
    return this.expression.hashCode();
  }

  @Override
  public String toString() {
    return this.expression.toString();
  }

}
