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

import cn.taketoday.lang.Nullable;

/**
 * Simple implementation of the {@link LocaleContext} interface,
 * always returning a specified {@code Locale}.
 *
 * @author Juergen Hoeller
 * @see LocaleContextHolder#setLocaleContext
 * @see LocaleContextHolder#getLocale()
 * @see SimpleTimeZoneAwareLocaleContext
 * @since 4.0
 */
public class SimpleLocaleContext implements LocaleContext {

  @Nullable
  private final Locale locale;

  /**
   * Create a new SimpleLocaleContext that exposes the specified Locale.
   * Every {@link #getLocale()} call will return this Locale.
   *
   * @param locale the Locale to expose, or {@code null} for no specific one
   */
  public SimpleLocaleContext(@Nullable Locale locale) {
    this.locale = locale;
  }

  @Override
  @Nullable
  public Locale getLocale() {
    return this.locale;
  }

  @Override
  public String toString() {
    return (this.locale != null ? this.locale.toString() : "-");
  }

}
