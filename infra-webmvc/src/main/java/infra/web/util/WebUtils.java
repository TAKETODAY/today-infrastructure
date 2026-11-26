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

package infra.web.util;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.TimeZone;

import infra.beans.factory.BeanFactoryUtils;
import infra.context.ApplicationContext;
import infra.core.Conventions;
import infra.core.i18n.LocaleContext;
import infra.core.i18n.LocaleContextHolder;
import infra.core.i18n.TimeZoneAwareLocaleContext;
import infra.http.HttpHeaders;
import infra.lang.Constant;
import infra.lang.Contract;
import infra.logging.LoggerFactory;
import infra.session.Session;
import infra.util.Assert;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;
import infra.util.StringUtils;
import infra.web.AbstractDelegatingServerHttpExchange;
import infra.web.LocaleContextResolver;
import infra.web.LocaleResolver;
import infra.web.RedirectModel;
import infra.web.RedirectModelManager;
import infra.web.ServerHttpExchange;
import infra.web.bind.MissingRequestParameterException;
import infra.web.bind.RequestBindingException;
import infra.web.multipart.Part;

/**
 * Miscellaneous utilities for web applications.
 * <p>Provides request parameter extraction, application locale resolution,
 * redirect model access, and utilities for sessions,
 * request paths, origins, and multipart cleanup.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class WebUtils {

  public static final String ERROR_MESSAGE_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          WebUtils.class, "error-message");

  public static final String ERROR_EXCEPTION_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          WebUtils.class, "error-exception");

  /**
   * Prefix of the charset clause in a content type String: ";charset=".
   */
  public static final String CONTENT_TYPE_CHARSET_PREFIX = ";charset=";

  /** Key for the mutex session attribute. */
  public static final String SESSION_MUTEX_ATTRIBUTE = WebUtils.class.getName() + ".MUTEX";

  /**
   * Return the native context of the given type, checking the supplied exchange
   * first and then unwrapping {@link AbstractDelegatingServerHttpExchange} instances.
   * Only the delegate chain is inspected; the outermost matching context wins.
   *
   * @param exchange the exchange to introspect
   * @param requiredType the desired type, or {@code null} for no match
   * @param <T> the type of the native context
   * @return the matching context, or {@code null} if the type is {@code null}
   * or no matching context is available
   * @see #nativeContext(ServerHttpExchange, Class)
   * @since 5.0
   */
  @SuppressWarnings("unchecked")
  public static <T> @Nullable T getNativeContext(ServerHttpExchange exchange, @Nullable Class<T> requiredType) {
    if (requiredType != null) {
      while (true) {
        if (requiredType.isInstance(exchange)) {
          return (T) exchange;
        }
        else if (exchange instanceof AbstractDelegatingServerHttpExchange wrapper) {
          exchange = wrapper.delegate();
        }
        else {
          break;
        }
      }
    }
    return null;
  }

  /**
   * Return the native context of the given type, throwing an exception if none
   * is available. The outermost matching context in the delegate chain wins.
   *
   * @param exchange the exchange to introspect
   * @param type the desired type of native context
   * @param <T> the type of the native context
   * @return the matching native context (never {@code null})
   * @throws IllegalStateException if no matching context is available
   * @see #getNativeContext(ServerHttpExchange, Class)
   * @since 5.0
   */
  public static <T> T nativeContext(ServerHttpExchange exchange, Class<T> type) {
    T nativeContext = getNativeContext(exchange, type);
    if (nativeContext == null) {
      throw new IllegalStateException("No native context of type " + type.getName() + " is available");
    }
    return nativeContext;
  }

  /**
   * Return the best available mutex for the given session:
   * that is, an object to synchronize on for the given session.
   *
   * <p>The session mutex is guaranteed to be the same object during
   * the entire lifetime of the session, available under the key defined
   * by the {@code SESSION_MUTEX_ATTRIBUTE} constant. It serves as a
   * safe reference to synchronize on for locking on the current session.
   * <p>In many cases, the Session reference itself is a safe mutex
   * as well, since it will always be the same object reference for the
   * same active logical session. However, this is not guaranteed across
   * different containers; the only 100% safe way is a session mutex.
   *
   * @param session the Session to find a mutex for
   * @return the mutex object (never {@code null})
   * @see #SESSION_MUTEX_ATTRIBUTE
   */
  public static Object getSessionMutex(Session session) {
    Assert.notNull(session, "Session is required");
    Object mutex = session.getAttribute(SESSION_MUTEX_ATTRIBUTE);
    if (mutex == null) {
      mutex = session;
    }
    return mutex;
  }

  /**
   * Parse the given string with matrix variables. An example string would look
   * like this {@code "q1=a;q1=b;q2=a,b,c"}. The resulting map would contain
   * keys {@code "q1"} and {@code "q2"} with values {@code ["a","b"]} and
   * {@code ["a","b","c"]} respectively.
   *
   * @param matrixVariables the unparsed matrix variables string
   * @return a map with matrix variable names and values (never {@code null})
   */
  public static MultiValueMap<String, String> parseMatrixVariables(String matrixVariables) {
    MultiValueMap<String, String> result = MultiValueMap.forLinkedHashMap();
    if (StringUtils.isBlank(matrixVariables)) {
      return result;
    }
    StringTokenizer pairs = new StringTokenizer(matrixVariables, ";");
    while (pairs.hasMoreTokens()) {
      String pair = pairs.nextToken();
      int index = pair.indexOf('=');
      if (index != -1) {
        String name = pair.substring(0, index);
        if (name.equalsIgnoreCase("jsessionid")) {
          continue;
        }
        String rawValue = pair.substring(index + 1);
        for (String value : StringUtils.commaDelimitedListToStringArray(rawValue)) {
          result.add(name, value);
        }
      }
      else {
        result.add(pair, "");
      }
    }
    return result;
  }

  // checkNotModified
  // ---------------------------------------------

  /**
   * Sanitize the given path. Uses the following rules:
   * <ul>
   * <li>replace all "//" by "/"</li>
   * </ul>
   */
  public static String getSanitizedPath(final String path) {
    int index = path.indexOf("//");
    if (index >= 0) {
      StringBuilder sanitized = new StringBuilder(path);
      while (index != -1) {
        sanitized.deleteCharAt(index);
        index = sanitized.indexOf("//", index);
      }
      return sanitized.toString();
    }
    return path;
  }

  /**
   * Remove ";" (semicolon) content from the given request URI
   *
   * @param lookupPath the request URI string to remove ";" content from
   * @return the updated URI string
   */
  public static String removeSemicolonContent(String lookupPath) {
    int semicolonIndex = lookupPath.indexOf(';');
    if (semicolonIndex == -1) {
      return lookupPath;
    }
    StringBuilder sb = new StringBuilder(lookupPath);
    while (semicolonIndex != -1) {
      int slashIndex = sb.indexOf("/", semicolonIndex + 1);
      if (slashIndex == -1) {
        return sb.substring(0, semicolonIndex);
      }
      sb.delete(semicolonIndex, slashIndex);
      semicolonIndex = sb.indexOf(";", semicolonIndex);
    }
    return sb.toString();
  }

  /**
   * Check the given request origin against a list of allowed origins.
   * A list containing "*" means that all origins are allowed.
   * An empty list means only same origin is allowed.
   *
   * <p><strong>Note:</strong> this method ignores
   * {@code "Forwarded"} and {@code "X-Forwarded-*"} headers that specify the
   * client-originated address. Consider using the {@code ForwardedHeaderFilter}
   * to extract and use, or to discard such headers.
   *
   * @return {@code true} if the request origin is valid, {@code false} otherwise
   * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
   */
  public static boolean isValidOrigin(ServerHttpExchange request, Collection<String> allowedOrigins) {
    Assert.notNull(request, "Request is required");
    Assert.notNull(allowedOrigins, "Allowed origins is required");

    String origin = request.getHeaders().getOrigin();
    if (origin == null || allowedOrigins.contains("*")) {
      return true;
    }
    else if (CollectionUtils.isEmpty(allowedOrigins)) {
      return isSameOrigin(request);
    }
    else {
      return allowedOrigins.contains(origin);
    }
  }

  /**
   * Check if the request is a same-origin one, based on {@code Origin}, {@code Host},
   * {@code Forwarded}, {@code X-Forwarded-Proto}, {@code X-Forwarded-Host} and
   * {@code X-Forwarded-Port} headers.
   *
   * <p><strong>Note:</strong> this method ignores
   * {@code "Forwarded"} and {@code "X-Forwarded-*"} headers that specify the
   * client-originated address. Consider using the {@code ForwardedHeaderFilter}
   * to extract and use, or to discard such headers.
   *
   * @return {@code true} if the request is a same-origin one, {@code false} in case
   * of cross-origin request
   */
  public static boolean isSameOrigin(ServerHttpExchange request) {
    HttpHeaders headers = request.getHeaders();
    String origin = headers.getOrigin();
    if (origin == null) {
      return true;
    }

    String scheme = request.getScheme();
    String host = request.getServerName();
    int port = request.getServerPort();

    UriComponents originUrl = UriComponentsBuilder.forOriginHeader(origin).build();
    return Objects.equals(scheme, originUrl.getScheme())
            && Objects.equals(host, originUrl.getHost())
            && getPort(scheme, port) == getPort(originUrl.getScheme(), originUrl.getPort());
  }

  private static int getPort(@Nullable String scheme, int port) {
    if (port == -1) {
      if ("http".equals(scheme) || "ws".equals(scheme)) {
        port = 80;
      }
      else if ("https".equals(scheme) || "wss".equals(scheme)) {
        port = 443;
      }
    }
    return port;
  }

  public static void cleanupMultipartRequest(@Nullable MultiValueMap<String, Part> map) {
    if (CollectionUtils.isNotEmpty(map)) {
      for (var entry : map.entrySet()) {
        for (Part part : entry.getValue()) {
          try {
            part.cleanup();
          }
          catch (Exception e) {
            LoggerFactory.getLogger(WebUtils.class)
                    .error("error occurred when cleanup multipart", e);
          }
        }
      }
    }
  }

  //---------------------------------------------------------------------
  // Request context utilities
  //---------------------------------------------------------------------

  /**
   * Find a bean by name in the application context associated with the request,
   * requiring it to match the specified type.
   * <p>Uses the application context's normal name lookup rules, including aliases
   * and delegation to a parent context. Does not fall back to a type-only lookup
   * or inspect request attributes.
   * <p>Returns {@code null} if the request has no application context or the
   * named bean is absent. If the bean exists, type mismatch and bean creation
   * exceptions are propagated rather than treated as an absent bean.
   *
   * @param request the HTTP request whose application context to search
   * @param beanName the name or alias of the bean to find
   * @param requiredType the required type of the bean
   * @param <T> the expected bean type
   * @return the bean, or {@code null} if no application context or named bean
   * is available
   * @throws infra.beans.factory.BeanNotOfRequiredTypeException if the named bean
   * does not match the required type
   * @throws infra.beans.BeansException if the bean cannot be obtained
   * @see ServerHttpExchange#getApplicationContext()
   * @see BeanFactoryUtils#find(infra.beans.factory.BeanFactory, String, Class)
   */
  public static <T> @Nullable T getBean(ServerHttpExchange request, String beanName, Class<T> requiredType) {
    ApplicationContext beanFactory = request.getApplicationContext();
    if (beanFactory != null) {
      return BeanFactoryUtils.find(beanFactory, beanName, requiredType);
    }
    return null;
  }

  /**
   * Find the {@link LocaleResolver#BEAN_NAME named LocaleResolver bean} in
   * the application context associated with the request.
   * <p>This method does not resolve a locale or read a request attribute.
   *
   * @param request current HTTP request
   * @return the configured resolver, or {@code null} if no application context
   * or matching bean is available
   */
  public static @Nullable LocaleResolver getLocaleResolver(ServerHttpExchange request) {
    return getBean(request, LocaleResolver.BEAN_NAME, LocaleResolver.class);
  }

  /**
   * Resolve the effective locale for application use, such as message lookup,
   * view rendering, and formatting.
   * <p>If a configured {@link #getLocaleResolver LocaleResolver} is available,
   * return its result directly: the resolver is responsible for its own defaults
   * and must return a non-null locale. Only when no resolver is available, use
   * {@link ServerHttpExchange#getPreferredLocale()} and, if that is null, {@link Locale#getDefault()}.
   * <p>Unlike {@link ServerHttpExchange#getPreferredLocale()}, this method always returns
   * a non-null locale on successful resolution. It does not suppress parsing
   * or resolver exceptions, or restrict the result to supported languages.
   * Configure application defaults and supported languages on the resolver.
   * <p>Resolver implementations must not call this method to obtain a fallback,
   * since doing so may invoke the same resolver recursively. They should read
   * {@link ServerHttpExchange#getPreferredLocale()} directly instead.
   * <p>Consider using {@link LocaleContextHolder#getLocale()}
   * which will normally be populated with the same Locale.
   *
   * @param exchange current HTTP exchange
   * @return the effective locale (never {@code null})
   * @see #getLocaleResolver
   * @see LocaleContextHolder#getLocale()
   */
  public static Locale resolveLocale(ServerHttpExchange exchange) {
    LocaleResolver localeResolver = getLocaleResolver(exchange);
    if (localeResolver != null) {
      return localeResolver.resolveLocale(exchange);
    }
    Locale locale = exchange.getPreferredLocale();
    return locale != null ? locale : Locale.getDefault();
  }

  /**
   * Retrieve the current time zone from the given request, using the
   * {@link TimeZoneAwareLocaleContext} supplied by the configured
   * {@link #getLocaleResolver LocaleResolver} (if available).
   * <p>Note: This method returns {@code null} if no specific time zone can be
   * resolved for the given request. This is in contrast to {@link #resolveLocale}
   * where it is always possible to fall back to the request's locale based on the
   * {@code Accept-Language} header or the default locale for the server.
   * <p>Consider using {@link LocaleContextHolder#getTimeZone()}
   * which will normally be populated with the same {@code TimeZone}: that method only
   * differs in terms of its fallback to the system time zone if the {@code LocaleResolver}
   * hasn't provided a specific time zone (instead of this method's {@code null}).
   *
   * @param exchange current HTTP exchange
   * @return the current time zone for the given request, either from the
   * {@code TimeZoneAwareLocaleContext} or {@code null} if none associated
   * @see #getLocaleResolver
   * @see LocaleContextHolder#getTimeZone()
   */
  public static @Nullable TimeZone getTimeZone(ServerHttpExchange exchange) {
    LocaleResolver localeResolver = getLocaleResolver(exchange);
    if (localeResolver instanceof LocaleContextResolver lcr) {
      LocaleContext localeContext = lcr.resolveLocaleContext(exchange);
      if (localeContext instanceof TimeZoneAwareLocaleContext) {
        return ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
      }
    }
    return null;
  }

  /**
   * Return "output" RedirectModel to save attributes for request after redirect.
   *
   * @param exchange current request
   * @return a {@link RedirectModel} instance
   */
  public static @Nullable RedirectModel getOutputRedirectModel(ServerHttpExchange exchange) {
    return RedirectModel.findOutputModel(exchange);
  }

  /**
   * Return the {@code RedirectModelManager} used to save and retrieve redirect models.
   * <p>the convenience method {@link #saveRedirectModel} may be
   * used to save the "output" RedirectModel.
   *
   * @param request the current request
   * @return a {@link RedirectModelManager} instance
   */
  public static @Nullable RedirectModelManager getRedirectModelManager(ServerHttpExchange request) {
    return getBean(request, RedirectModelManager.BEAN_NAME, RedirectModelManager.class);
  }

  /**
   * Convenience method that retrieves the {@link #getOutputRedirectModel "output"
   * RedirectModel}, updates it with the path and query params of the target URL,
   * and then saves it using the {@link #getRedirectModelManager RedirectModelManager}.
   *
   * @param location the target URL for the redirect
   * @param request the current request
   */
  public static void saveRedirectModel(String location, ServerHttpExchange request) {
    saveRedirectModel(location, request, null);
  }

  /**
   * Convenience method that retrieves the {@link #getOutputRedirectModel "output"
   * RedirectModel}, updates it with the path and query params of the target URL,
   * and then saves it using the {@link #getRedirectModelManager RedirectModelManager}.
   *
   * @param location the target URL for the redirect
   * @param request the current request
   * @param manager RedirectModelManager
   */
  public static void saveRedirectModel(String location, ServerHttpExchange request, @Nullable RedirectModelManager manager) {
    RedirectModel redirectModel = getOutputRedirectModel(request);
    if (CollectionUtils.isNotEmpty(redirectModel)) {
      if (manager == null) {
        manager = getRedirectModelManager(request);
      }
      if (manager != null) {
        UriComponents uriComponents = UriComponentsBuilder.forURIString(location).build();
        redirectModel.setTargetRequestPath(uriComponents.getPath());
        redirectModel.addTargetRequestParams(uriComponents.getQueryParams());

        manager.saveRedirectModel(request, redirectModel);
      }
    }
  }

  //---------------------------------------------------------------------
  // Request parameters
  //---------------------------------------------------------------------

  /**
   * Get an Integer parameter, or {@code null} if not present.
   * Throws an exception if it the parameter value isn't a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @return the Integer value, or {@code null} if not present
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static @Nullable Integer getIntParameter(ServerHttpExchange request, String name)
          throws RequestBindingException {

    if (request.getParameter(name) == null) {
      return null;
    }
    return getRequiredIntParameter(request, name);
  }

  /**
   * Get an int parameter, with a fallback value. Never throws an exception.
   * Can pass a distinguished value as default to enable checks of whether it was supplied.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @param defaultVal the default value to use as fallback
   */
  public static int getIntParameter(ServerHttpExchange request, String name, int defaultVal) {
    if (request.getParameter(name) == null) {
      return defaultVal;
    }
    try {
      return getRequiredIntParameter(request, name);
    }
    catch (RequestBindingException ex) {
      return defaultVal;
    }
  }

  /**
   * Get an array of int parameters, return an empty array if not found.
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   */
  public static int[] getIntParameters(ServerHttpExchange request, String name) {
    try {
      return getRequiredIntParameters(request, name);
    }
    catch (RequestBindingException ex) {
      return new int[0];
    }
  }

  /**
   * Get an int parameter, throwing an exception if it isn't found or isn't a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static int getRequiredIntParameter(ServerHttpExchange request, String name)
          throws RequestBindingException {

    return IntParser.INSTANCE.parseInt(name, request.getParameter(name));
  }

  /**
   * Get an array of int parameters, throwing an exception if not found or one is not a number..
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static int[] getRequiredIntParameters(ServerHttpExchange request, String name)
          throws RequestBindingException {

    return IntParser.INSTANCE.parseInts(name, request.getParameterValues(name));
  }

  /**
   * Get a Long parameter, or {@code null} if not present.
   * Throws an exception if it the parameter value isn't a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @return the Long value, or {@code null} if not present
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static @Nullable Long getLongParameter(ServerHttpExchange request, String name)
          throws RequestBindingException {

    if (request.getParameter(name) == null) {
      return null;
    }
    return getRequiredLongParameter(request, name);
  }

  /**
   * Get a long parameter, with a fallback value. Never throws an exception.
   * Can pass a distinguished value as default to enable checks of whether it was supplied.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @param defaultVal the default value to use as fallback
   */
  public static long getLongParameter(ServerHttpExchange request, String name, long defaultVal) {
    if (request.getParameter(name) == null) {
      return defaultVal;
    }
    try {
      return getRequiredLongParameter(request, name);
    }
    catch (RequestBindingException ex) {
      return defaultVal;
    }
  }

  /**
   * Get an array of long parameters, return an empty array if not found.
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   */
  public static long[] getLongParameters(ServerHttpExchange request, String name) {
    try {
      return getRequiredLongParameters(request, name);
    }
    catch (RequestBindingException ex) {
      return new long[0];
    }
  }

  /**
   * Get a long parameter, throwing an exception if it isn't found or isn't a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static long getRequiredLongParameter(ServerHttpExchange request, String name)
          throws RequestBindingException {

    return LongParser.INSTANCE.parseLong(name, request.getParameter(name));
  }

  /**
   * Get an array of long parameters, throwing an exception if not found or one is not a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static long[] getRequiredLongParameters(ServerHttpExchange request, String name)
          throws RequestBindingException {

    return LongParser.INSTANCE.parseLongs(name, request.getParameterValues(name));
  }

  /**
   * Get a Float parameter, or {@code null} if not present.
   * Throws an exception if it the parameter value isn't a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @return the Float value, or {@code null} if not present
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static @Nullable Float getFloatParameter(ServerHttpExchange request, String name)
          throws RequestBindingException {

    if (request.getParameter(name) == null) {
      return null;
    }
    return getRequiredFloatParameter(request, name);
  }

  /**
   * Get a float parameter, with a fallback value. Never throws an exception.
   * Can pass a distinguished value as default to enable checks of whether it was supplied.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @param defaultVal the default value to use as fallback
   */
  public static float getFloatParameter(ServerHttpExchange request, String name, float defaultVal) {
    if (request.getParameter(name) == null) {
      return defaultVal;
    }
    try {
      return getRequiredFloatParameter(request, name);
    }
    catch (RequestBindingException ex) {
      return defaultVal;
    }
  }

  /**
   * Get an array of float parameters, return an empty array if not found.
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   */
  public static float[] getFloatParameters(ServerHttpExchange request, String name) {
    try {
      return getRequiredFloatParameters(request, name);
    }
    catch (RequestBindingException ex) {
      return new float[0];
    }
  }

  /**
   * Get a float parameter, throwing an exception if it isn't found or isn't a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static float getRequiredFloatParameter(ServerHttpExchange request, String name)
          throws RequestBindingException {

    return FloatParser.INSTANCE.parseFloat(name, request.getParameter(name));
  }

  /**
   * Get an array of float parameters, throwing an exception if not found or one is not a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static float[] getRequiredFloatParameters(ServerHttpExchange request, String name)
          throws RequestBindingException {

    return FloatParser.INSTANCE.parseFloats(name, request.getParameterValues(name));
  }

  /**
   * Get a Double parameter, or {@code null} if not present.
   * Throws an exception if it the parameter value isn't a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @return the Double value, or {@code null} if not present
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static @Nullable Double getDoubleParameter(ServerHttpExchange request, String name)
          throws RequestBindingException {

    if (request.getParameter(name) == null) {
      return null;
    }
    return getRequiredDoubleParameter(request, name);
  }

  /**
   * Get a double parameter, with a fallback value. Never throws an exception.
   * Can pass a distinguished value as default to enable checks of whether it was supplied.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @param defaultVal the default value to use as fallback
   */
  public static double getDoubleParameter(ServerHttpExchange request, String name, double defaultVal) {
    if (request.getParameter(name) == null) {
      return defaultVal;
    }
    try {
      return getRequiredDoubleParameter(request, name);
    }
    catch (RequestBindingException ex) {
      return defaultVal;
    }
  }

  /**
   * Get an array of double parameters, return an empty array if not found.
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   */
  public static double[] getDoubleParameters(ServerHttpExchange request, String name) {
    try {
      return getRequiredDoubleParameters(request, name);
    }
    catch (RequestBindingException ex) {
      return new double[0];
    }
  }

  /**
   * Get a double parameter, throwing an exception if it isn't found or isn't a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static double getRequiredDoubleParameter(ServerHttpExchange request, String name)
          throws RequestBindingException {

    return DoubleParser.INSTANCE.parseDouble(name, request.getParameter(name));
  }

  /**
   * Get an array of double parameters, throwing an exception if not found or one is not a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static double[] getRequiredDoubleParameters(ServerHttpExchange request, String name)
          throws RequestBindingException {

    return DoubleParser.INSTANCE.parseDoubles(name, request.getParameterValues(name));
  }

  /**
   * Get a Boolean parameter, or {@code null} if not present.
   * Throws an exception if it the parameter value isn't a boolean.
   * <p>Accepts "true", "on", "yes" (any case) and "1" as values for true;
   * treats every other non-empty value as false (i.e. parses leniently).
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @return the Boolean value, or {@code null} if not present
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static @Nullable Boolean getBooleanParameter(ServerHttpExchange request, String name)
          throws RequestBindingException {

    if (request.getParameter(name) == null) {
      return null;
    }
    return getRequiredBooleanParameter(request, name);
  }

  /**
   * Get a boolean parameter, with a fallback value. Never throws an exception.
   * Can pass a distinguished value as default to enable checks of whether it was supplied.
   * <p>Accepts "true", "on", "yes" (any case) and "1" as values for true;
   * treats every other non-empty value as false (i.e. parses leniently).
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @param defaultVal the default value to use as fallback
   */
  public static boolean getBooleanParameter(ServerHttpExchange request, String name, boolean defaultVal) {
    if (request.getParameter(name) == null) {
      return defaultVal;
    }
    try {
      return getRequiredBooleanParameter(request, name);
    }
    catch (RequestBindingException ex) {
      return defaultVal;
    }
  }

  /**
   * Get an array of boolean parameters, return an empty array if not found.
   * <p>Accepts "true", "on", "yes" (any case) and "1" as values for true;
   * treats every other non-empty value as false (i.e. parses leniently).
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   */
  public static boolean[] getBooleanParameters(ServerHttpExchange request, String name) {
    try {
      return getRequiredBooleanParameters(request, name);
    }
    catch (RequestBindingException ex) {
      return new boolean[0];
    }
  }

  /**
   * Get a boolean parameter, throwing an exception if it isn't found
   * or isn't a boolean.
   * <p>Accepts "true", "on", "yes" (any case) and "1" as values for true;
   * treats every other non-empty value as false (i.e. parses leniently).
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static boolean getRequiredBooleanParameter(ServerHttpExchange request, String name)
          throws RequestBindingException {

    return BooleanParser.INSTANCE.parseBoolean(name, request.getParameter(name));
  }

  /**
   * Get an array of boolean parameters, throwing an exception if not found
   * or one isn't a boolean.
   * <p>Accepts "true", "on", "yes" (any case) and "1" as values for true;
   * treats every other non-empty value as false (i.e. parses leniently).
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static boolean[] getRequiredBooleanParameters(ServerHttpExchange request, String name)
          throws RequestBindingException {
    return BooleanParser.INSTANCE.parseBooleans(name, request.getParameterValues(name));
  }

  /**
   * Get a String parameter, or {@code null} if not present.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @return the String value, or {@code null} if not present
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static @Nullable String getStringParameter(ServerHttpExchange request, String name)
          throws RequestBindingException {

    if (request.getParameter(name) == null) {
      return null;
    }
    return getRequiredStringParameter(request, name);
  }

  /**
   * Get a String parameter, with a fallback value. Never throws an exception.
   * Can pass a distinguished value to default to enable checks of whether it was supplied.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @param defaultVal the default value to use as fallback
   */
  public static String getStringParameter(ServerHttpExchange request, String name, String defaultVal) {
    String val = request.getParameter(name);
    return (val != null ? val : defaultVal);
  }

  /**
   * Get an array of String parameters, return an empty array if not found.
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   */
  public static String[] getStringParameters(ServerHttpExchange request, String name) {
    try {
      return getRequiredStringParameters(request, name);
    }
    catch (RequestBindingException ex) {
      return Constant.EMPTY_STRING_ARRAY;
    }
  }

  /**
   * Get a String parameter, throwing an exception if it isn't found.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static String getRequiredStringParameter(ServerHttpExchange request, String name)
          throws RequestBindingException {

    return StringParser.INSTANCE.validateRequiredString(name, request.getParameter(name));
  }

  /**
   * Get an array of String parameters, throwing an exception if not found.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @throws RequestBindingException a subclass of NestedRuntimeException,
   * so it doesn't need to be caught
   */
  public static String[] getRequiredStringParameters(ServerHttpExchange request, String name)
          throws RequestBindingException {

    return StringParser.INSTANCE.validateRequiredStrings(name, request.getParameterValues(name));
  }

  private abstract static class ParameterParser<T> {

    protected final T parse(String name, @Nullable String parameter) throws RequestBindingException {
      validateRequiredParameter(name, parameter);
      try {
        return doParse(parameter);
      }
      catch (NumberFormatException ex) {
        throw new RequestBindingException("Required %s parameter '%s' with value of '%s' is not a valid number"
                .formatted(getType(), name, parameter), ex);
      }
    }

    @Contract("_, null -> fail")
    protected final void validateRequiredParameter(String name, @Nullable Object parameter)
            throws RequestBindingException {

      if (parameter == null) {
        throw new MissingRequestParameterException(name, getType());
      }
    }

    protected abstract String getType();

    protected abstract T doParse(String parameter) throws NumberFormatException;
  }

  private static final class IntParser extends ParameterParser<Integer> {

    private static final IntParser INSTANCE = new IntParser();

    @Override
    protected String getType() {
      return "int";
    }

    @Override
    protected Integer doParse(String s) throws NumberFormatException {
      return Integer.valueOf(s);
    }

    public int parseInt(String name, @Nullable String parameter) throws RequestBindingException {
      return parse(name, parameter);
    }

    public int[] parseInts(String name, String @Nullable [] values) throws RequestBindingException {
      validateRequiredParameter(name, values);
      int[] parameters = new int[values.length];
      for (int i = 0; i < values.length; i++) {
        parameters[i] = parseInt(name, values[i]);
      }
      return parameters;
    }
  }

  private static final class LongParser extends ParameterParser<Long> {

    private static final LongParser INSTANCE = new LongParser();

    @Override
    protected String getType() {
      return "long";
    }

    @Override
    protected Long doParse(String parameter) throws NumberFormatException {
      return Long.valueOf(parameter);
    }

    public long parseLong(String name, @Nullable String parameter) throws RequestBindingException {
      return parse(name, parameter);
    }

    public long[] parseLongs(String name, String @Nullable [] values) throws RequestBindingException {
      validateRequiredParameter(name, values);
      long[] parameters = new long[values.length];
      for (int i = 0; i < values.length; i++) {
        parameters[i] = parseLong(name, values[i]);
      }
      return parameters;
    }
  }

  private static final class FloatParser extends ParameterParser<Float> {

    private static final FloatParser INSTANCE = new FloatParser();

    @Override
    protected String getType() {
      return "float";
    }

    @Override
    protected Float doParse(String parameter) throws NumberFormatException {
      return Float.valueOf(parameter);
    }

    public float parseFloat(String name, @Nullable String parameter) throws RequestBindingException {
      return parse(name, parameter);
    }

    public float[] parseFloats(String name, String @Nullable [] values) throws RequestBindingException {
      validateRequiredParameter(name, values);
      float[] parameters = new float[values.length];
      for (int i = 0; i < values.length; i++) {
        parameters[i] = parseFloat(name, values[i]);
      }
      return parameters;
    }
  }

  private static final class DoubleParser extends ParameterParser<Double> {

    private static final DoubleParser INSTANCE = new DoubleParser();

    @Override
    protected String getType() {
      return "double";
    }

    @Override
    protected Double doParse(String parameter) throws NumberFormatException {
      return Double.valueOf(parameter);
    }

    public double parseDouble(String name, @Nullable String parameter) throws RequestBindingException {
      return parse(name, parameter);
    }

    public double[] parseDoubles(String name, String @Nullable [] values) throws RequestBindingException {
      validateRequiredParameter(name, values);
      double[] parameters = new double[values.length];
      for (int i = 0; i < values.length; i++) {
        parameters[i] = parseDouble(name, values[i]);
      }
      return parameters;
    }
  }

  private static final class BooleanParser extends ParameterParser<Boolean> {

    private static final BooleanParser INSTANCE = new BooleanParser();

    @Override
    protected String getType() {
      return "boolean";
    }

    @Override
    protected Boolean doParse(String parameter) throws NumberFormatException {
      return (parameter.equalsIgnoreCase("true") || parameter.equalsIgnoreCase("on") ||
              parameter.equalsIgnoreCase("yes") || parameter.equals("1"));
    }

    public boolean parseBoolean(String name, @Nullable String parameter) throws RequestBindingException {
      return parse(name, parameter);
    }

    public boolean[] parseBooleans(String name, String @Nullable [] values) throws RequestBindingException {
      validateRequiredParameter(name, values);
      boolean[] parameters = new boolean[values.length];
      for (int i = 0; i < values.length; i++) {
        parameters[i] = parseBoolean(name, values[i]);
      }
      return parameters;
    }
  }

  private static final class StringParser extends ParameterParser<String> {

    private static final StringParser INSTANCE = new StringParser();

    @Override
    protected String getType() {
      return "string";
    }

    @Override
    protected String doParse(String parameter) throws NumberFormatException {
      return parameter;
    }

    public String validateRequiredString(String name, @Nullable String value) throws RequestBindingException {
      validateRequiredParameter(name, value);
      return value;
    }

    @SuppressWarnings("NullAway")
    public String[] validateRequiredStrings(String name, String @Nullable [] values) throws RequestBindingException {
      validateRequiredParameter(name, values);
      for (String value : values) {
        validateRequiredParameter(name, value);
      }
      return values;
    }
  }

}
