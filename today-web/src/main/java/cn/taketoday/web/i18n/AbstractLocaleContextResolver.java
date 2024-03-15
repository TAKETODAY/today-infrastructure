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
import java.util.TimeZone;

import cn.taketoday.core.i18n.SimpleLocaleContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.LocaleContextResolver;
import cn.taketoday.web.RequestContext;

/**
 * Abstract base class for {@link LocaleContextResolver} implementations.
 * Provides support for a default locale and a default time zone.
 *
 * <p>Also provides pre-implemented versions of {@link #resolveLocale} and {@link #setLocale},
 * delegating to {@link #resolveLocaleContext} and {@link #setLocaleContext}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 * @since 4.0
 */
public abstract class AbstractLocaleContextResolver
        extends AbstractLocaleResolver implements LocaleContextResolver {

  @Nullable
  private TimeZone defaultTimeZone;

  /**
   * Set a default TimeZone that this resolver will return if no other time zone found.
   */
  public void setDefaultTimeZone(@Nullable TimeZone defaultTimeZone) {
    this.defaultTimeZone = defaultTimeZone;
  }

  /**
   * Return the default TimeZone that this resolver is supposed to fall back to, if any.
   */
  @Nullable
  public TimeZone getDefaultTimeZone() {
    return this.defaultTimeZone;
  }

  @Override
  public Locale resolveLocale(RequestContext request) {
    Locale locale = resolveLocaleContext(request).getLocale();
    return locale != null ? locale : request.getLocale();
  }

  @Override
  public void setLocale(RequestContext request, @Nullable Locale locale) {
    setLocaleContext(request, locale != null ? new SimpleLocaleContext(locale) : null);
  }

}
