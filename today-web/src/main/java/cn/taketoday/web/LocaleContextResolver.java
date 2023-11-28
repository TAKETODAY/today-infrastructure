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

package cn.taketoday.web;

import java.util.Locale;

import cn.taketoday.core.i18n.LocaleContext;
import cn.taketoday.lang.Nullable;

/**
 * Extension of {@link LocaleResolver}, adding support for a rich locale context
 * (potentially including locale and time zone information).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.core.i18n.LocaleContext
 * @see cn.taketoday.core.i18n.TimeZoneAwareLocaleContext
 * @see cn.taketoday.core.i18n.LocaleContextHolder
 * @see cn.taketoday.web.RequestContextUtils#getTimeZone
 * @since 4.0 2022/2/3 22:54
 */
public interface LocaleContextResolver extends LocaleResolver {

  /**
   * Resolve the current locale context via the given request.
   * <p>This is primarily intended for framework-level processing; consider using
   * {@link cn.taketoday.web.RequestContextUtils} or
   * {@link cn.taketoday.web.RequestContext} for
   * application-level access to the current locale and/or time zone.
   * <p>The returned context may be a
   * {@link cn.taketoday.core.i18n.TimeZoneAwareLocaleContext},
   * containing a locale with associated time zone information.
   * Simply apply an {@code instanceof} check and downcast accordingly.
   * <p>Custom resolver implementations may also return extra settings in
   * the returned context, which again can be accessed through downcasting.
   *
   * @param request the request to resolve the locale context for
   * @return the current locale context (never {@code null}
   * @see #resolveLocale(RequestContext)
   * @see cn.taketoday.web.RequestContextUtils#getLocale
   * @see cn.taketoday.web.RequestContextUtils#getTimeZone
   */
  LocaleContext resolveLocaleContext(RequestContext request);

  /**
   * Set the current locale context to the given one,
   * potentially including a locale with associated time zone information.
   *
   * @param request the request to be used for locale modification
   * @param localeContext the new locale context, or {@code null} to clear the locale
   * @throws UnsupportedOperationException if the LocaleResolver implementation
   * does not support dynamic changing of the locale or time zone
   * @see #setLocale(RequestContext, Locale)
   * @see cn.taketoday.core.i18n.SimpleLocaleContext
   * @see cn.taketoday.core.i18n.SimpleTimeZoneAwareLocaleContext
   */
  void setLocaleContext(RequestContext request, @Nullable LocaleContext localeContext);

}
