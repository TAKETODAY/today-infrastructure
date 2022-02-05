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
import cn.taketoday.core.i18n.SimpleLocaleContext;
import cn.taketoday.core.i18n.TimeZoneAwareLocaleContext;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.LocaleContextResolver;
import cn.taketoday.web.LocaleResolver;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.CookieGenerator;
import cn.taketoday.web.util.WebUtils;

/**
 * {@link LocaleResolver} implementation that uses a cookie sent back to the user
 * in case of a custom setting, with a fallback to the specified default locale
 * or the request's accept-header locale.
 *
 * <p>This is particularly useful for stateless applications without user sessions.
 * The cookie may optionally contain an associated time zone value as well;
 * alternatively, you may specify a default time zone.
 *
 * <p>Custom controllers can override the user's locale and time zone by calling
 * {@code #setLocale(Context)} on the resolver, e.g. responding to a locale change
 * request.
 *
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 * @since 4.0
 */
public class CookieLocaleResolver extends CookieGenerator implements LocaleContextResolver {

  /**
   * The name of the request attribute that holds the {@code Locale}.
   * <p>Only used for overriding a cookie value if the locale has been
   * changed in the course of the current request!
   * <p>Use {@code RequestContext(Utils).getLocale()}
   * to retrieve the current locale in controllers or views.
   *
   * @see cn.taketoday.web.RequestContext#getLocale
   * @see cn.taketoday.web.RequestContextUtils#getLocale
   */
  public static final String LOCALE_REQUEST_ATTRIBUTE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";

  /**
   * The name of the request attribute that holds the {@code TimeZone}.
   * <p>Only used for overriding a cookie value if the locale has been
   * changed in the course of the current request!
   * <p>Use {@code RequestContext(Utils).getTimeZone()}
   * to retrieve the current time zone in controllers or views.
   *
   * @see cn.taketoday.web.RequestContextUtils#getTimeZone
   */
  public static final String TIME_ZONE_REQUEST_ATTRIBUTE_NAME = CookieLocaleResolver.class.getName() + ".TIME_ZONE";

  /**
   * The default cookie name used if none is explicitly set.
   */
  public static final String DEFAULT_COOKIE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";

  private boolean languageTagCompliant = true;

  private boolean rejectInvalidCookies = true;

  @Nullable
  private Locale defaultLocale;

  @Nullable
  private TimeZone defaultTimeZone;

  /**
   * Create a new instance of the {@link CookieLocaleResolver} class
   * using the {@link #DEFAULT_COOKIE_NAME default cookie name}.
   */
  public CookieLocaleResolver() {
    setCookieName(DEFAULT_COOKIE_NAME);
  }

  /**
   * Specify whether this resolver's cookies should be compliant with BCP 47
   * language tags instead of Java's legacy locale specification format.
   * <p>The default is {@code true}, as of 5.1. Switch this to {@code false}
   * for rendering Java's legacy locale specification format. For parsing,
   * this resolver leniently accepts the legacy {@link Locale#toString}
   * format as well as BCP 47 language tags in any case.
   *
   * @see #parseLocaleValue(String)
   * @see #toLocaleValue(Locale)
   * @see Locale#forLanguageTag(String)
   * @see Locale#toLanguageTag()
   */
  public void setLanguageTagCompliant(boolean languageTagCompliant) {
    this.languageTagCompliant = languageTagCompliant;
  }

  /**
   * Return whether this resolver's cookies should be compliant with BCP 47
   * language tags instead of Java's legacy locale specification format.
   */
  public boolean isLanguageTagCompliant() {
    return this.languageTagCompliant;
  }

  /**
   * Specify whether to reject cookies with invalid content (e.g. invalid format).
   * <p>The default is {@code true}. Turn this off for lenient handling of parse
   * failures, falling back to the default locale and time zone in such a case.
   *
   * @see #setDefaultLocale
   * @see #setDefaultTimeZone
   * @see #determineDefaultLocale
   * @see #determineDefaultTimeZone
   */
  public void setRejectInvalidCookies(boolean rejectInvalidCookies) {
    this.rejectInvalidCookies = rejectInvalidCookies;
  }

  /**
   * Return whether to reject cookies with invalid content (e.g. invalid format).
   */
  public boolean isRejectInvalidCookies() {
    return this.rejectInvalidCookies;
  }

  /**
   * Set a fixed locale that this resolver will return if no cookie found.
   */
  public void setDefaultLocale(@Nullable Locale defaultLocale) {
    this.defaultLocale = defaultLocale;
  }

  /**
   * Return the fixed locale that this resolver will return if no cookie found,
   * if any.
   */
  @Nullable
  protected Locale getDefaultLocale() {
    return this.defaultLocale;
  }

  /**
   * Set a fixed time zone that this resolver will return if no cookie found.
   */
  public void setDefaultTimeZone(@Nullable TimeZone defaultTimeZone) {
    this.defaultTimeZone = defaultTimeZone;
  }

  /**
   * Return the fixed time zone that this resolver will return if no cookie found,
   * if any.
   */
  @Nullable
  protected TimeZone getDefaultTimeZone() {
    return this.defaultTimeZone;
  }

  @Override
  public Locale resolveLocale(RequestContext request) {
    parseLocaleCookieIfNecessary(request);
    return (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
  }

  @Override
  public LocaleContext resolveLocaleContext(final RequestContext request) {
    parseLocaleCookieIfNecessary(request);
    return new TimeZoneAwareLocaleContext() {
      @Override
      @Nullable
      public Locale getLocale() {
        return (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
      }

      @Override
      @Nullable
      public TimeZone getTimeZone() {
        return (TimeZone) request.getAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
      }
    };
  }

  private void parseLocaleCookieIfNecessary(RequestContext request) {
    if (request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME) == null) {
      Locale locale = null;
      TimeZone timeZone = null;

      // Retrieve and parse cookie value.
      String cookieName = getCookieName();
      if (cookieName != null) {
        HttpCookie cookie = WebUtils.getCookie(request, cookieName);
        if (cookie != null) {
          String value = cookie.getValue();
          String localePart = value;
          String timeZonePart = null;
          int separatorIndex = localePart.indexOf('/');
          if (separatorIndex == -1) {
            // Leniently accept older cookies separated by a space...
            separatorIndex = localePart.indexOf(' ');
          }
          if (separatorIndex >= 0) {
            localePart = value.substring(0, separatorIndex);
            timeZonePart = value.substring(separatorIndex + 1);
          }
          try {
            locale = (!"-".equals(localePart) ? parseLocaleValue(localePart) : null);
            if (timeZonePart != null) {
              timeZone = StringUtils.parseTimeZoneString(timeZonePart);
            }
          }
          catch (IllegalArgumentException ex) {
            if (isRejectInvalidCookies() &&
                    request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) == null) {
              throw new IllegalStateException("Encountered invalid locale cookie '" +
                      cookieName + "': [" + value + "] due to: " + ex.getMessage());
            }
            else {
              // Lenient handling (e.g. error dispatch): ignore locale/timezone parse exceptions
              if (logger.isDebugEnabled()) {
                logger.debug("Ignoring invalid locale cookie '" + cookieName +
                        "': [" + value + "] due to: " + ex.getMessage());
              }
            }
          }
          if (logger.isTraceEnabled()) {
            logger.trace("Parsed cookie value [" + cookie.getValue() + "] into locale '" + locale +
                    "'" + (timeZone != null ? " and time zone '" + timeZone.getID() + "'" : ""));
          }
        }
      }

      request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME,
              (locale != null ? locale : determineDefaultLocale(request)));
      request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME,
              (timeZone != null ? timeZone : determineDefaultTimeZone(request)));
    }
  }

  @Override
  public void setLocale(RequestContext request, @Nullable Locale locale) {
    setLocaleContext(request, (locale != null ? new SimpleLocaleContext(locale) : null));
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
      addCookie(request,
              (locale != null ? toLocaleValue(locale) : "-") + (timeZone != null ? '/' + timeZone.getID() : ""));
    }
    else {
      removeCookie(request);
    }
    request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME,
            (locale != null ? locale : determineDefaultLocale(request)));
    request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME,
            (timeZone != null ? timeZone : determineDefaultTimeZone(request)));
  }

  /**
   * Parse the given locale value coming from an incoming cookie.
   * <p>The default implementation calls {@link StringUtils#parseLocale(String)},
   * accepting the {@link Locale#toString} format as well as BCP 47 language tags.
   *
   * @param localeValue the locale value to parse
   * @return the corresponding {@code Locale} instance
   * @see StringUtils#parseLocale(String)
   */
  @Nullable
  protected Locale parseLocaleValue(String localeValue) {
    return StringUtils.parseLocale(localeValue);
  }

  /**
   * Render the given locale as a text value for inclusion in a cookie.
   * <p>The default implementation calls {@link Locale#toString()}
   * or JDK 7's {@link Locale#toLanguageTag()}, depending on the
   * {@link #setLanguageTagCompliant "languageTagCompliant"} configuration property.
   *
   * @param locale the locale to stringify
   * @return a String representation for the given locale
   * @see #isLanguageTagCompliant()
   */
  protected String toLocaleValue(Locale locale) {
    return isLanguageTagCompliant() ? locale.toLanguageTag() : locale.toString();
  }

  /**
   * Determine the default locale for the given request,
   * Called if no locale cookie has been found.
   * <p>The default implementation returns the specified default locale,
   * if any, else falls back to the request's accept-header locale.
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
   * Determine the default time zone for the given request,
   * Called if no time zone cookie has been found.
   * <p>The default implementation returns the specified default time zone,
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
