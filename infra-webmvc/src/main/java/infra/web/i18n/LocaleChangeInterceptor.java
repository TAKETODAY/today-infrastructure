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
  public boolean preProcessing(RequestContext request, Object handler) {
    String newLocale = request.getParameter(getParamName());
    if (newLocale != null) {
      if (checkHttpMethod(request.getMethodAsString())) {
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
