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

import infra.core.i18n.LocaleContext;
import infra.core.i18n.TimeZoneAwareLocaleContext;
import infra.web.RequestContext;

/**
 * {@link infra.web.LocaleResolver} implementation
 * that always returns a fixed default locale and optionally time zone.
 * Default is the current JVM's default locale.
 *
 * <p>Note: Does not support {@code setLocale(Context)}, as the fixed
 * locale and time zone cannot be changed.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 * @since 4.0
 */
public class FixedLocaleResolver extends AbstractLocaleContextResolver {

  /**
   * Create a default FixedLocaleResolver, exposing a configured default
   * locale (or the JVM's default locale as fallback).
   *
   * @see #setDefaultLocale
   * @see #setDefaultTimeZone
   */
  public FixedLocaleResolver() {
    setDefaultLocale(Locale.getDefault());
  }

  /**
   * Create a FixedLocaleResolver that exposes the given locale.
   *
   * @param locale the locale to expose
   */
  public FixedLocaleResolver(@Nullable Locale locale) {
    setDefaultLocale(locale);
  }

  /**
   * Create a FixedLocaleResolver that exposes the given locale and time zone.
   *
   * @param locale the locale to expose
   * @param timeZone the time zone to expose
   */
  public FixedLocaleResolver(@Nullable Locale locale, @Nullable TimeZone timeZone) {
    setDefaultLocale(locale);
    setDefaultTimeZone(timeZone);
  }

  @Override
  public Locale resolveLocale(RequestContext request) {
    Locale locale = getDefaultLocale();
    if (locale == null) {
      locale = Locale.getDefault();
    }
    return locale;
  }

  @Override
  public LocaleContext resolveLocaleContext(RequestContext request) {
    return new TimeZoneAwareLocaleContext() {
      @Override
      @Nullable
      public Locale getLocale() {
        return getDefaultLocale();
      }

      @Nullable
      @Override
      public TimeZone getTimeZone() {
        return getDefaultTimeZone();
      }
    };
  }

  @Override
  public void setLocaleContext(RequestContext request, @Nullable LocaleContext localeContext) {
    throw new UnsupportedOperationException("Cannot change fixed locale - use a different locale resolution strategy");
  }

}
