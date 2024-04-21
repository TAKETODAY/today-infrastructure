/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
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
 * {@link LocaleResolver} implementation that looks for a match between locales
 * in the {@code Accept-Language} header and a list of configured supported
 * locales.
 *
 * <p>See {@link #setSupportedLocales(List)} for further details on how
 * supported and requested locales are matched.
 *
 * <p>Note: This implementation does not support {@link #setLocale} since the
 * {@code Accept-Language} header can only be changed by changing the client's
 * locale settings.
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
   * Configure the list of supported locales to compare and match against
   * {@link RequestContext#getLocale() requested locales}.
   * <p>In order for a supported locale to be considered a match, it must match
   * on both country and language. If you want to support a language-only match
   * as a fallback, you must configure the language explicitly as a supported
   * locale.
   * <p>For example, if the supported locales are {@code ["de-DE","en-US"]},
   * then a request for {@code "en-GB"} will not match, and neither will a
   * request for {@code "en"}. If you want to support additional locales for a
   * given language such as {@code "en"}, then you must add it to the list of
   * supported locales.
   * <p>If there is no match, then the {@link #setDefaultLocale(Locale)
   * defaultLocale} is used, if configured, or otherwise falling back on
   * {@link RequestContext#getLocale()}.
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
