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

package cn.taketoday.format.datetime.standard;

import java.time.ZoneId;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import cn.taketoday.core.i18n.LocaleContext;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.core.i18n.TimeZoneAwareLocaleContext;
import cn.taketoday.lang.Nullable;

/**
 * A context that holds user-specific <code>java.time</code> (JSR-310) settings
 * such as the user's Chronology (calendar system) and time zone.
 * <p>A {@code null} property value indicates the user has not specified a setting.
 *
 * @author Juergen Hoeller
 * @see DateTimeContextHolder
 * @since 4.0
 */
public class DateTimeContext {

  @Nullable
  private Chronology chronology;

  @Nullable
  private ZoneId timeZone;

  /**
   * Set the user's chronology (calendar system).
   */
  public void setChronology(@Nullable Chronology chronology) {
    this.chronology = chronology;
  }

  /**
   * Return the user's chronology (calendar system), if any.
   */
  @Nullable
  public Chronology getChronology() {
    return this.chronology;
  }

  /**
   * Set the user's time zone.
   * <p>Alternatively, set a {@link TimeZoneAwareLocaleContext} on
   * {@link LocaleContextHolder}. This context class will fall back to
   * checking the locale context if no setting has been provided here.
   *
   * @see cn.taketoday.core.i18n.LocaleContextHolder#getTimeZone()
   * @see cn.taketoday.core.i18n.LocaleContextHolder#setLocaleContext
   */
  public void setTimeZone(@Nullable ZoneId timeZone) {
    this.timeZone = timeZone;
  }

  /**
   * Return the user's time zone, if any.
   */
  @Nullable
  public ZoneId getTimeZone() {
    return this.timeZone;
  }

  /**
   * Get the DateTimeFormatter with this context's settings applied to the
   * base {@code formatter}.
   *
   * @param formatter the base formatter that establishes default
   * formatting rules, generally context-independent
   * @return the contextual DateTimeFormatter
   */
  public DateTimeFormatter getFormatter(DateTimeFormatter formatter) {
    if (this.chronology != null) {
      formatter = formatter.withChronology(this.chronology);
    }
    if (this.timeZone != null) {
      formatter = formatter.withZone(this.timeZone);
    }
    else {
      LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
      if (localeContext instanceof TimeZoneAwareLocaleContext) {
        TimeZone timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
        if (timeZone != null) {
          formatter = formatter.withZone(timeZone.toZoneId());
        }
      }
    }
    return formatter;
  }

}
