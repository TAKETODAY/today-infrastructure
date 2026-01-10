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

package infra.web;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

import infra.core.i18n.LocaleContext;
import infra.core.i18n.LocaleContextHolder;
import infra.core.i18n.SimpleLocaleContext;
import infra.core.i18n.SimpleTimeZoneAwareLocaleContext;
import infra.core.i18n.TimeZoneAwareLocaleContext;

/**
 * Extension of {@link LocaleResolver}, adding support for a rich locale context
 * (potentially including locale and time zone information).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LocaleContext
 * @see TimeZoneAwareLocaleContext
 * @see LocaleContextHolder
 * @see infra.web.RequestContextUtils#getTimeZone
 * @since 4.0 2022/2/3 22:54
 */
public interface LocaleContextResolver extends LocaleResolver {

  /**
   * Resolve the current locale context via the given request.
   * <p>This is primarily intended for framework-level processing; consider using
   * {@link infra.web.RequestContextUtils} or
   * {@link infra.web.RequestContext} for
   * application-level access to the current locale and/or time zone.
   * <p>The returned context may be a
   * {@link TimeZoneAwareLocaleContext},
   * containing a locale with associated time zone information.
   * Simply apply an {@code instanceof} check and downcast accordingly.
   * <p>Custom resolver implementations may also return extra settings in
   * the returned context, which again can be accessed through downcasting.
   *
   * @param request the request to resolve the locale context for
   * @return the current locale context (never {@code null}
   * @see #resolveLocale(RequestContext)
   * @see infra.web.RequestContextUtils#getLocale
   * @see infra.web.RequestContextUtils#getTimeZone
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
   * @see SimpleLocaleContext
   * @see SimpleTimeZoneAwareLocaleContext
   */
  void setLocaleContext(RequestContext request, @Nullable LocaleContext localeContext);

}
