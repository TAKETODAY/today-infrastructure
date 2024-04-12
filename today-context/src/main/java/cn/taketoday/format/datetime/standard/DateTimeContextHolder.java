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

package cn.taketoday.format.datetime.standard;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import cn.taketoday.core.NamedThreadLocal;
import cn.taketoday.lang.Nullable;

/**
 * A holder for a thread-local user {@link DateTimeContext}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.core.i18n.LocaleContextHolder
 * @since 4.0
 */
public final class DateTimeContextHolder {

  private static final ThreadLocal<DateTimeContext> dateTimeContextHolder =
          new NamedThreadLocal<>("DateTimeContext");

  private DateTimeContextHolder() { }

  /**
   * Reset the DateTimeContext for the current thread.
   */
  public static void resetDateTimeContext() {
    dateTimeContextHolder.remove();
  }

  /**
   * Associate the given DateTimeContext with the current thread.
   *
   * @param dateTimeContext the current DateTimeContext,
   * or {@code null} to reset the thread-bound context
   */
  public static void setDateTimeContext(@Nullable DateTimeContext dateTimeContext) {
    if (dateTimeContext == null) {
      resetDateTimeContext();
    }
    else {
      dateTimeContextHolder.set(dateTimeContext);
    }
  }

  /**
   * Return the DateTimeContext associated with the current thread, if any.
   *
   * @return the current DateTimeContext, or {@code null} if none
   */
  @Nullable
  public static DateTimeContext getDateTimeContext() {
    return dateTimeContextHolder.get();
  }

  /**
   * Obtain a DateTimeFormatter with user-specific settings applied to the given base formatter.
   *
   * @param formatter the base formatter that establishes default formatting rules
   * (generally user independent)
   * @param locale the current user locale (may be {@code null} if not known)
   * @return the user-specific DateTimeFormatter
   */
  public static DateTimeFormatter getFormatter(DateTimeFormatter formatter, @Nullable Locale locale) {
    if (locale != null) {
      formatter = formatter.withLocale(locale);
    }
    DateTimeContext context = getDateTimeContext();
    if (context != null) {
      formatter = context.getFormatter(formatter);
    }
    return formatter;
  }

}
