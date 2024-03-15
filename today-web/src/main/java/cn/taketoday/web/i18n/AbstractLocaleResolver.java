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

package cn.taketoday.web.i18n;

import java.util.Locale;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.LocaleResolver;

/**
 * Abstract base class for {@link LocaleResolver} implementations.
 * Provides support for a default locale.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setDefaultLocale
 * @since 4.0
 */
public abstract class AbstractLocaleResolver implements LocaleResolver {

  @Nullable
  private Locale defaultLocale;

  /**
   * Set a default Locale that this resolver will return if no other locale found.
   */
  public void setDefaultLocale(@Nullable Locale defaultLocale) {
    this.defaultLocale = defaultLocale;
  }

  /**
   * Return the default Locale that this resolver is supposed to fall back to, if any.
   */
  @Nullable
  protected Locale getDefaultLocale() {
    return this.defaultLocale;
  }

}
