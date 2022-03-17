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

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.LocaleResolver;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.interceptor.HandlerInterceptor;

/**
 * Interceptor that allows for changing the current locale on every request,
 * via a configurable request parameter (default parameter name: "locale").
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see cn.taketoday.web.LocaleResolver
 * @since 4.0
 */
public class LocaleChangeInterceptor implements HandlerInterceptor {

  /**
   * Default name of the locale specification parameter: "locale".
   */
  public static final String DEFAULT_PARAM_NAME = "locale";

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private String paramName = DEFAULT_PARAM_NAME;

  @Nullable
  private String[] httpMethods;

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
  public void setHttpMethods(@Nullable String... httpMethods) {
    this.httpMethods = httpMethods;
  }

  /**
   * Return the configured HTTP methods.
   */
  @Nullable
  public String[] getHttpMethods() {
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
  public boolean beforeProcess(RequestContext context, Object handler) {
    String newLocale = context.getParameter(getParamName());
    if (newLocale != null) {
      if (checkHttpMethod(context.getMethodValue())) {
        LocaleResolver localeResolver = getLocaleResolver();
        if (localeResolver == null) {
          throw new IllegalStateException("No LocaleResolver found");
        }
        try {
          localeResolver.setLocale(context, parseLocaleValue(newLocale));
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
