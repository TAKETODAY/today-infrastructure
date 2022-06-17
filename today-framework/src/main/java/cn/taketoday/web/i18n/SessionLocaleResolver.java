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

package cn.taketoday.web.i18n;

import java.util.Locale;
import java.util.TimeZone;

import cn.taketoday.core.i18n.LocaleContext;
import cn.taketoday.core.i18n.TimeZoneAwareLocaleContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.session.SessionManager;
import cn.taketoday.web.session.WebSession;

/**
 * {@link cn.taketoday.web.LocaleResolver} implementation that
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
 * chosen locale settings in the Servlet container's {@code HttpSession}. As a
 * consequence, those settings are just temporary for each session and therefore
 * lost when each session terminates.
 *
 * <p>Note that there is no direct relationship with external session management
 * mechanisms such as the "Session" project. This {@code LocaleResolver}
 * will simply evaluate and modify corresponding {@code HttpSession} attributes
 * against the current {@code RequestContext}.
 *
 * @author Juergen Hoeller
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 * @since 4.0
 */
public class SessionLocaleResolver extends AbstractLocaleContextResolver {

  /**
   * Default name of the session attribute that holds the Locale.
   * Only used internally by this implementation.
   * <p>Use {@code RequestContext(Utils).getLocale()}
   * to retrieve the current locale in controllers or views.
   *
   * @see cn.taketoday.web.RequestContext#getLocale
   * @see cn.taketoday.web.RequestContextUtils#getLocale
   */
  public static final String LOCALE_SESSION_ATTRIBUTE_NAME = SessionLocaleResolver.class.getName() + ".LOCALE";

  /**
   * Default name of the session attribute that holds the TimeZone.
   * Only used internally by this implementation.
   * <p>Use {@code RequestContext(Utils).getTimeZone()}
   * to retrieve the current time zone in controllers or views.
   *
   * @see cn.taketoday.web.RequestContextUtils#getTimeZone
   */
  public static final String TIME_ZONE_SESSION_ATTRIBUTE_NAME = SessionLocaleResolver.class.getName() + ".TIME_ZONE";

  private String localeAttributeName = LOCALE_SESSION_ATTRIBUTE_NAME;

  private String timeZoneAttributeName = TIME_ZONE_SESSION_ATTRIBUTE_NAME;

  @Nullable
  private SessionManager sessionManager;

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

  public void setSessionManager(@Nullable SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  @Nullable
  public SessionManager getSessionManager() {
    return sessionManager;
  }

  @Override
  public Locale resolveLocale(RequestContext request) {
    Locale locale = getSessionAttribute(request, this.localeAttributeName);
    if (locale == null) {
      locale = determineDefaultLocale(request);
    }
    return locale;
  }

  @Override
  public LocaleContext resolveLocaleContext(final RequestContext request) {
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
      @Nullable
      public TimeZone getTimeZone() {
        TimeZone timeZone = getSessionAttribute(request, timeZoneAttributeName);
        if (timeZone == null) {
          timeZone = determineDefaultTimeZone(request);
        }
        return timeZone;
      }
    };
  }

  @Override
  public void setLocaleContext(RequestContext request, @Nullable LocaleContext localeContext) {
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

  @Nullable
  @SuppressWarnings("unchecked")
  private <T> T getSessionAttribute(RequestContext request, String attributeName) {
    SessionManager sessionManager = getSessionManager(request);
    if (sessionManager != null) {
      WebSession session = sessionManager.getSession(request, false);
      if (session != null) {
        return (T) session.getAttribute(attributeName);
      }
    }
    return null;
  }

  private void setSessionAttribute(
          RequestContext request, String attributeName, @Nullable Object attribute) {
    SessionManager sessionManager = getSessionManager(request);
    if (sessionManager != null) {
      WebSession session = sessionManager.getSession(request);
      session.setAttribute(attributeName, attribute);
    }
  }

  @Nullable
  private SessionManager getSessionManager(RequestContext request) {
    SessionManager sessionManager = getSessionManager();
    if (sessionManager == null) {
      sessionManager = RequestContextUtils.getSessionManager(request);
    }
    return sessionManager;
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
   * @see RequestContext#getLocale()
   */
  protected Locale determineDefaultLocale(RequestContext request) {
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
  @Nullable
  protected TimeZone determineDefaultTimeZone(RequestContext request) {
    return getDefaultTimeZone();
  }

}
