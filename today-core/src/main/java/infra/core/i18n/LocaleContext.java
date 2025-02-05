/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.core.i18n;

import java.util.Locale;

import infra.lang.Nullable;

/**
 * Strategy interface for determining the current Locale.
 *
 * <p>A LocaleContext instance can be associated with a thread
 * via the LocaleContextHolder class.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see LocaleContextHolder#getLocale()
 * @see TimeZoneAwareLocaleContext
 * @since 4.0
 */
public interface LocaleContext {

  /**
   * Return the current Locale, which can be fixed or determined dynamically,
   * depending on the implementation strategy.
   *
   * @return the current Locale, or {@code null} if no specific Locale associated
   */
  @Nullable
  Locale getLocale();

}
