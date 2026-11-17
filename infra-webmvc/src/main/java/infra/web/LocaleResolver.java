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

/**
 * Interface for web-based locale resolution strategies that allows for
 * both locale resolution via the request and locale modification via
 * request and response.
 *
 * <p>This interface allows for implementations based on request, session,
 * cookies, etc. The default implementation is
 * {@link infra.web.i18n.AcceptHeaderLocaleResolver},
 * simply using the request's locale provided by the respective HTTP header.
 *
 * <p>Use {@link HttpContextUtils#resolveLocale(HttpContext)}
 * to retrieve the current locale in controllers or views, independent
 * of the actual resolution strategy.
 * <p>In contrast, {@link HttpContext#getLocale()} only exposes the request's
 * {@code Accept-Language} preference and may return {@code null}. Implementations
 * may use that preference as an input, but must supply their own fallback to
 * satisfy the non-null resolution contract. They must not call
 * {@link HttpContextUtils#resolveLocale(HttpContext)} for a fallback, since that
 * may invoke the same resolver recursively.
 * <p>Register the application strategy under {@link #BEAN_NAME}. Changing a
 * locale through this strategy does not modify the request's language header
 * or the value exposed by {@link HttpContext#getLocale()}.
 *
 * <p>there is an extended strategy interface
 * called {@link LocaleContextResolver}, allowing for resolution of
 * a {@link LocaleContext} object,
 * potentially including associated time zone information. Framework's
 * provided resolver implementations implement the extended
 * {@link LocaleContextResolver} interface wherever appropriate.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LocaleContextResolver
 * @see LocaleContextHolder
 * @see HttpContext#getLocale
 * @see HttpContextUtils#resolveLocale
 * @since 4.0 2022/2/3 22:54
 */
public interface LocaleResolver {

  /**
   * Bean name of the locale resolver used by MVC.
   */
  String BEAN_NAME = "mvcLocaleResolver";

  /**
   * Resolve the effective application locale for the given request, applying
   * this strategy's configured defaults when necessary.
   * <p>The result may differ from {@link HttpContext#getLocale()}, for example
   * when a cookie or session contains an explicit language selection.
   *
   * @param context the request to resolve the locale for
   * @return the current locale (never {@code null})
   */
  Locale resolveLocale(HttpContext context);

  /**
   * Set the current locale to the given one.
   *
   * @param context the request to be used for locale modification
   * @param locale the new locale, or {@code null} to clear the locale
   * @throws UnsupportedOperationException if the LocaleResolver
   * implementation does not support dynamic changing of the locale
   */
  void setLocale(HttpContext context, @Nullable Locale locale);

}
