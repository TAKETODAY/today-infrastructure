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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.LocaleResolver;
import cn.taketoday.web.RequestContext;

/**
 * {@link LocaleResolver} implementation that simply uses the primary locale
 * specified in the "accept-language" header of the HTTP request (that is,
 * the locale sent by the client browser, normally that of the client's OS).
 *
 * <p>Note: Does not support {@code setLocale}, since the accept header
 * can only be changed through changing the client's locale settings.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see RequestContext#getLocale()
 * @since 4.0
 */
public class AcceptHeaderLocaleResolver implements LocaleResolver {

  private final List<Locale> supportedLocales = new ArrayList<>(4);

  @Nullable
  private Locale defaultLocale;

  /**
   * Configure supported locales to check against the requested locales
   * determined via {@link HttpHeaders#getAcceptLanguageAsLocales()} ()}. If this is not
   * configured then {@link RequestContext#getLocale()} is used instead.
   *
   * @param locales the supported locales
   */
  public void setSupportedLocales(List<Locale> locales) {
    this.supportedLocales.clear();
    this.supportedLocales.addAll(locales);
  }

  /**
   * Return the configured list of supported locales.
   */
  public List<Locale> getSupportedLocales() {
    return this.supportedLocales;
  }

  /**
   * Configure a fixed default locale to fall back on if the request does not
   * have an "Accept-Language" header.
   * <p>By default this is not set in which case when there is no "Accept-Language"
   * header, the default locale for the server is used as defined in
   * {@link RequestContext#getLocale()}.
   *
   * @param defaultLocale the default locale to use
   */
  public void setDefaultLocale(@Nullable Locale defaultLocale) {
    this.defaultLocale = defaultLocale;
  }

  /**
   * The configured default locale, if any.
   * <p>This method may be overridden in subclasses.
   */
  @Nullable
  public Locale getDefaultLocale() {
    return this.defaultLocale;
  }

  @Override
  public Locale resolveLocale(RequestContext request) {
    Locale defaultLocale = getDefaultLocale();
    if (defaultLocale != null && request.requestHeaders().get(HttpHeaders.ACCEPT_LANGUAGE) == null) {
      return defaultLocale;
    }
    Locale requestLocale = request.getLocale();
    List<Locale> supportedLocales = getSupportedLocales();
    if (supportedLocales.isEmpty() || supportedLocales.contains(requestLocale)) {
      return requestLocale;
    }
    Locale supportedLocale = findSupportedLocale(request, supportedLocales);
    if (supportedLocale != null) {
      return supportedLocale;
    }
    return defaultLocale != null ? defaultLocale : requestLocale;
  }

  @Nullable
  private Locale findSupportedLocale(RequestContext request, List<Locale> supportedLocales) {
    Locale languageMatch = null;
    List<Locale> requestLocales = request.requestHeaders().getAcceptLanguageAsLocales();
    for (Locale locale : requestLocales) {
      if (supportedLocales.contains(locale)) {
        if (languageMatch == null || languageMatch.getLanguage().equals(locale.getLanguage())) {
          // Full match: language + country, possibly narrowed from earlier language-only match
          return locale;
        }
      }
      else if (languageMatch == null) {
        // Let's try to find a language-only match as a fallback
        for (Locale candidate : supportedLocales) {
          if (StringUtils.isEmpty(candidate.getCountry())
                  && candidate.getLanguage().equals(locale.getLanguage())) {
            languageMatch = candidate;
            break;
          }
        }
      }
    }
    return languageMatch;
  }

  @Override
  public void setLocale(RequestContext request, @Nullable Locale locale) {
    throw new UnsupportedOperationException(
            "Cannot change HTTP accept header - use a different locale resolution strategy");
  }

}
