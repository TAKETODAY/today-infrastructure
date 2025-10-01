/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.i18n;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.HandlerInterceptor;
import infra.web.LocaleResolver;
import infra.web.RequestContext;
import infra.web.RequestContextUtils;

/**
 * Interceptor that allows for changing the current locale on every request,
 * via a configurable request parameter (default parameter name: "locale").
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.web.LocaleResolver
 * @since 4.0
 */
public class LocaleChangeInterceptor implements HandlerInterceptor {

  /**
   * Default name of the locale specification parameter: "locale".
   */
  public static final String DEFAULT_PARAM_NAME = "locale";

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private String paramName = DEFAULT_PARAM_NAME;

  private String @Nullable [] httpMethods;

  private boolean ignoreInvalidLocale = false;

  @Nullable
  private LocaleResolver localeResolver;

  /**
   * Set the name of the parameter that contains a locale specification
   * in a locale change request. Default is "locale".
   */
  public void setParamName(String paramName) {
    this.paramName = paramName;
  }

  /**
   * Return the name of the parameter that contains a locale specification
   * in a locale change request.
   */
  public String getParamName() {
    return this.paramName;
  }

  /**
   * Configure the HTTP method(s) over which the locale can be changed.
   *
   * @param httpMethods the methods
   */
  public void setHttpMethods(String @Nullable ... httpMethods) {
    this.httpMethods = httpMethods;
  }

  /**
   * Return the configured HTTP methods.
   */
  public String @Nullable [] getHttpMethods() {
    return this.httpMethods;
  }

  /**
   * Set whether to ignore an invalid value for the locale parameter.
   */
  public void setIgnoreInvalidLocale(boolean ignoreInvalidLocale) {
    this.ignoreInvalidLocale = ignoreInvalidLocale;
  }

  /**
   * Return whether to ignore an invalid value for the locale parameter.
   */
  public boolean isIgnoreInvalidLocale() {
    return this.ignoreInvalidLocale;
  }

  /**
   * Configure LocaleResolver
   */
  public void setLocaleResolver(@Nullable LocaleResolver localeResolver) {
    this.localeResolver = localeResolver;
  }

  /**
   * Return LocaleResolver.
   */
  @Nullable
  public LocaleResolver getLocaleResolver() {
    return localeResolver;
  }

  @Override
  public boolean beforeProcess(RequestContext request, Object handler) {
    String newLocale = request.getParameter(getParamName());
    if (newLocale != null) {
      if (checkHttpMethod(request.getMethodValue())) {
        LocaleResolver localeResolver = getLocaleResolver();
        if (localeResolver == null) {
          localeResolver = RequestContextUtils.getLocaleResolver(request);
          if (localeResolver == null) {
            throw new IllegalStateException("No LocaleResolver found");
          }
        }
        try {
          localeResolver.setLocale(request, parseLocaleValue(newLocale));
        }
        catch (IllegalArgumentException ex) {
          if (isIgnoreInvalidLocale()) {
            if (logger.isDebugEnabled()) {
              logger.debug("Ignoring invalid locale value [{}]: {}", newLocale, ex.getMessage());
            }
          }
          else {
            throw ex;
          }
        }
      }
    }
    // Proceed in any case.
    return true;
  }

  private boolean checkHttpMethod(String currentMethod) {
    String[] configuredMethods = getHttpMethods();
    if (ObjectUtils.isEmpty(configuredMethods)) {
      return true;
    }
    for (String configuredMethod : configuredMethods) {
      if (configuredMethod.equalsIgnoreCase(currentMethod)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Parse the given locale value as coming from a request parameter.
   * <p>The default implementation calls {@link StringUtils#parseLocale(String)},
   * accepting the {@link Locale#toString} format as well as BCP 47 language tags.
   *
   * @param localeValue the locale value to parse
   * @return the corresponding {@code Locale} instance
   */
  @Nullable
  protected Locale parseLocaleValue(String localeValue) {
    return StringUtils.parseLocale(localeValue);
  }

}
