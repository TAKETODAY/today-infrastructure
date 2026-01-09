/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.format.datetime.standard;

import org.jspecify.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import infra.core.NamedThreadLocal;
import infra.core.i18n.LocaleContextHolder;

/**
 * A holder for a thread-local user {@link DateTimeContext}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LocaleContextHolder
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
