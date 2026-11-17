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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import infra.util.StringUtils;
import infra.web.HttpContext;
import infra.web.LocaleResolver;

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
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see HttpContext#getLocale()
 * @since 4.0
 */
public class AcceptHeaderLocaleResolver implements LocaleResolver {

  private final List<Locale> supportedLocales = new ArrayList<>(4);

  private @Nullable Locale defaultLocale;

  /**
   * Configure the list of supported locales to compare and match against
   * {@link HttpContext#getLocale() requested locales}.
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
   * the request locale or {@link Locale#getDefault()} if no request locale is available.
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
   * Configure a fixed default locale to use if the request has no acceptable
   * concrete language range or none of its ranges matches the supported locales.
   * <p>If not configured, an absent request locale falls back to
   * {@link Locale#getDefault()}, while a non-matching request falls back to its
   * preferred locale. A supported-locale list alone is therefore not a strict
   * allowlist: configure a default locale as well to handle non-matching requests.
   * <p>Wildcard and zero-quality ranges are ignored. Invalid language headers
   * still raise parsing exceptions rather than selecting this default.
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
  public @Nullable Locale getDefaultLocale() {
    return this.defaultLocale;
  }

  @Override
  public Locale resolveLocale(HttpContext context) {
    Locale defaultLocale = getDefaultLocale();
    Locale requestLocale = context.getLocale();
    if (requestLocale == null) {
      return defaultLocale != null ? defaultLocale : Locale.getDefault();
    }
    List<Locale> supportedLocales = getSupportedLocales();
    if (supportedLocales.isEmpty() || supportedLocales.contains(requestLocale)) {
      return requestLocale;
    }
    Locale supportedLocale = findSupportedLocale(context, supportedLocales);
    if (supportedLocale != null) {
      return supportedLocale;
    }
    return defaultLocale != null ? defaultLocale : requestLocale;
  }

  private @Nullable Locale findSupportedLocale(HttpContext request, List<Locale> supportedLocales) {
    Locale languageMatch = null;
    for (Locale.LanguageRange range : request.requestHeaders().getAcceptLanguage()) {
      if (range.getWeight() == 0 || range.getRange().contains("*")) {
        continue;
      }
      Locale locale = Locale.forLanguageTag(range.getRange());
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
  public void setLocale(HttpContext context, @Nullable Locale locale) {
    throw new UnsupportedOperationException(
            "Cannot change HTTP accept header - use a different locale resolution strategy");
  }

}
