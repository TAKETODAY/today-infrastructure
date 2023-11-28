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

import cn.taketoday.lang.Nullable;

/**
 * Interface for web-based locale resolution strategies that allows for
 * both locale resolution via the request and locale modification via
 * request and response.
 *
 * <p>This interface allows for implementations based on request, session,
 * cookies, etc. The default implementation is
 * {@link cn.taketoday.web.i18n.AcceptHeaderLocaleResolver},
 * simply using the request's locale provided by the respective HTTP header.
 *
 * <p>Use {@link cn.taketoday.web.RequestContext#getLocale()}
 * to retrieve the current locale in controllers or views, independent
 * of the actual resolution strategy.
 *
 * <p>there is an extended strategy interface
 * called {@link LocaleContextResolver}, allowing for resolution of
 * a {@link cn.taketoday.core.i18n.LocaleContext} object,
 * potentially including associated time zone information. Framework's
 * provided resolver implementations implement the extended
 * {@link LocaleContextResolver} interface wherever appropriate.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LocaleContextResolver
 * @see cn.taketoday.core.i18n.LocaleContextHolder
 * @see cn.taketoday.web.RequestContext#getLocale
 * @see cn.taketoday.web.RequestContextUtils#getLocale
 * @since 4.0 2022/2/3 22:54
 */
public interface LocaleResolver {

  /**
   * default bean name
   */
  String BEAN_NAME = "webLocaleResolver";

  /**
   * Resolve the current locale via the given request.
   * Can return a default locale as fallback in any case.
   *
   * @param request the request to resolve the locale for
   * @return the current locale (never {@code null})
   */
  Locale resolveLocale(RequestContext request);

  /**
   * Set the current locale to the given one.
   *
   * @param request the request to be used for locale modification
   * @param locale the new locale, or {@code null} to clear the locale
   * @throws UnsupportedOperationException if the LocaleResolver
   * implementation does not support dynamic changing of the locale
   */
  void setLocale(RequestContext request, @Nullable Locale locale);

}

