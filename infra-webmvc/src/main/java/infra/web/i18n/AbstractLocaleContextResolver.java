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

package infra.web.i18n;

import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.TimeZone;

import infra.core.i18n.SimpleLocaleContext;
import infra.web.LocaleContextResolver;
import infra.web.RequestContext;

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
