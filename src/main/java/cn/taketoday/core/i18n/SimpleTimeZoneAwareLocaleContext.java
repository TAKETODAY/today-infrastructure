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

package cn.taketoday.core.i18n;

import java.util.Locale;
import java.util.TimeZone;

import cn.taketoday.lang.Nullable;

/**
 * Simple implementation of the {@link TimeZoneAwareLocaleContext} interface,
 * always returning a specified {@code Locale} and {@code TimeZone}.
 *
 * <p>Note: Prefer the use of {@link SimpleLocaleContext} when only setting
 * a Locale but no TimeZone.
 *
 * @author Juergen Hoeller
 * @author Nicholas Williams
 * @see LocaleContextHolder#setLocaleContext
 * @see LocaleContextHolder#getTimeZone()
 * @since 4.0
 */
public class SimpleTimeZoneAwareLocaleContext extends SimpleLocaleContext implements TimeZoneAwareLocaleContext {

  @Nullable
  private final TimeZone timeZone;

  /**
   * Create a new SimpleTimeZoneAwareLocaleContext that exposes the specified
   * Locale and TimeZone. Every {@link #getLocale()} call will return the given
   * Locale, and every {@link #getTimeZone()} call will return the given TimeZone.
   *
   * @param locale the Locale to expose
   * @param timeZone the TimeZone to expose
   */
  public SimpleTimeZoneAwareLocaleContext(@Nullable Locale locale, @Nullable TimeZone timeZone) {
    super(locale);
    this.timeZone = timeZone;
  }

  @Override
  @Nullable
  public TimeZone getTimeZone() {
    return this.timeZone;
  }

  @Override
  public String toString() {
    return super.toString() + " " + (this.timeZone != null ? this.timeZone.toString() : "-");
  }

}
