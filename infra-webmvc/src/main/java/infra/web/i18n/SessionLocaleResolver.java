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
import infra.lang.Assert;
import infra.session.Session;
import infra.web.HttpContext;
import infra.web.HttpContextUtils;

/**
 * {@link infra.web.LocaleResolver} implementation that
 * uses a locale attribute in the user's session in case of a custom setting,
 * with a fallback to the configured default locale, the request's
 * {@code Accept-Language} header, or the default locale for the server.
 *
 * <p>This is most appropriate if the application needs user sessions anyway,
 * i.e. when the {@code HttpSession} does not have to be created just for storing
 * the user's locale. The session may optionally contain an associated time zone
 * attribute as well; alternatively, you may specify a default time zone.
 *
 * <p>In contrast to {@link CookieLocaleResolver}, this strategy stores locally
 * chosen locale settings in the {@code Session}. As a
 * consequence, those settings are just temporary for each session and therefore
 * lost when each session terminates.
 *
 * <p>Note that there is no direct relationship with external session management
 * mechanisms such as the "Session" project. This {@code LocaleResolver}
 * will simply evaluate and modify corresponding {@code HttpSession} attributes
 * against the current {@code HttpContext}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 * @since 4.0
 */
public class SessionLocaleResolver extends AbstractLocaleContextResolver {

  /**
   * Default name of the session attribute that holds the Locale.
   * Only used internally by this implementation.
   * <p>Use {@code HttpContext(Utils).getLocale()}
   * to retrieve the current locale in controllers or views.
   *
   * @see HttpContext#getLocale
   * @see HttpContextUtils#getLocale
   */
  public static final String LOCALE_SESSION_ATTRIBUTE_NAME = SessionLocaleResolver.class.getName() + ".LOCALE";

  /**
   * Default name of the session attribute that holds the TimeZone.
   * Only used internally by this implementation.
   * <p>Use {@code HttpContext(Utils).getTimeZone()}
   * to retrieve the current time zone in controllers or views.
   *
   * @see HttpContextUtils#getTimeZone
   */
  public static final String TIME_ZONE_SESSION_ATTRIBUTE_NAME = SessionLocaleResolver.class.getName() + ".TIME_ZONE";

  private String localeAttributeName = LOCALE_SESSION_ATTRIBUTE_NAME;

  private String timeZoneAttributeName = TIME_ZONE_SESSION_ATTRIBUTE_NAME;

  /**
   * Specify the name of the corresponding attribute in the {@code HttpSession},
   * holding the current {@link Locale} value.
   * <p>The default is an internal {@link #LOCALE_SESSION_ATTRIBUTE_NAME}.
   */
  public void setLocaleAttributeName(String localeAttributeName) {
    Assert.notNull(localeAttributeName, "localeAttributeName is required");
    this.localeAttributeName = localeAttributeName;
  }

  /**
   * Specify the name of the corresponding attribute in the {@code HttpSession},
   * holding the current {@link TimeZone} value.
   * <p>The default is an internal {@link #TIME_ZONE_SESSION_ATTRIBUTE_NAME}.
   */
  public void setTimeZoneAttributeName(String timeZoneAttributeName) {
    Assert.notNull(timeZoneAttributeName, "timeZoneAttributeName is required");
    this.timeZoneAttributeName = timeZoneAttributeName;
  }

  @Override
  public Locale resolveLocale(HttpContext request) {
    Locale locale = getSessionAttribute(request, this.localeAttributeName);
    if (locale == null) {
      locale = determineDefaultLocale(request);
    }
    return locale;
  }

  @Override
  public LocaleContext resolveLocaleContext(final HttpContext request) {
    return new TimeZoneAwareLocaleContext() {
      @Override
      public Locale getLocale() {
        Locale locale = getSessionAttribute(request, localeAttributeName);
        if (locale == null) {
          locale = determineDefaultLocale(request);
        }
        return locale;
      }

      @Override
      public @Nullable TimeZone getTimeZone() {
        TimeZone timeZone = getSessionAttribute(request, timeZoneAttributeName);
        if (timeZone == null) {
          timeZone = determineDefaultTimeZone(request);
        }
        return timeZone;
      }
    };
  }

  @Override
  public void setLocaleContext(HttpContext request, @Nullable LocaleContext localeContext) {
    Locale locale = null;
    TimeZone timeZone = null;
    if (localeContext != null) {
      locale = localeContext.getLocale();
      if (localeContext instanceof TimeZoneAwareLocaleContext) {
        timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
      }
    }
    setSessionAttribute(request, localeAttributeName, locale);
    setSessionAttribute(request, timeZoneAttributeName, timeZone);
  }

  @SuppressWarnings("unchecked")
  private <T> @Nullable T getSessionAttribute(HttpContext request, String attributeName) {
    Session session = request.getSession(false);
    if (session != null) {
      return (T) session.getAttribute(attributeName);
    }
    return null;
  }

  private void setSessionAttribute(HttpContext request, String attributeName, @Nullable Object attribute) {
    Session session = request.getSession();
    session.setAttribute(attributeName, attribute);
  }

  /**
   * Determine the default locale for the given request, called if no
   * {@link Locale} session attribute has been found.
   * <p>The default implementation returns the configured
   * {@linkplain #setDefaultLocale(Locale) default locale}, if any, and otherwise
   * falls back to the request's {@code Accept-Language} header locale or the
   * default locale for the server.
   *
   * @param request the request to resolve the locale for
   * @return the default locale (never {@code null})
   * @see #setDefaultLocale
   * @see HttpContext#getLocale()
   */
  protected Locale determineDefaultLocale(HttpContext request) {
    Locale defaultLocale = getDefaultLocale();
    if (defaultLocale == null) {
      defaultLocale = request.getLocale();
    }
    return defaultLocale;
  }

  /**
   * Determine the default time zone for the given request, called if no
   * {@link TimeZone} session attribute has been found.
   * <p>The default implementation returns the configured default time zone,
   * if any, or {@code null} otherwise.
   *
   * @param request the request to resolve the time zone for
   * @return the default time zone (or {@code null} if none defined)
   * @see #setDefaultTimeZone
   */
  protected @Nullable TimeZone determineDefaultTimeZone(HttpContext request) {
    return getDefaultTimeZone();
  }

}
