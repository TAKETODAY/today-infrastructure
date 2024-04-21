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

package cn.taketoday.web;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.i18n.LocaleContext;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.core.i18n.TimeZoneAwareLocaleContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.session.SessionManager;
import cn.taketoday.session.WebSession;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.bind.MissingRequestParameterException;
import cn.taketoday.web.bind.RequestBindingException;
import cn.taketoday.web.context.support.RequestScope;
import cn.taketoday.web.context.support.SessionManagerDiscover;
import cn.taketoday.web.context.support.SessionScope;
import cn.taketoday.web.util.UriComponents;
import cn.taketoday.web.util.UriComponentsBuilder;

/**
 * Parameter extraction methods, for an approach distinct from data binding,
 * in which parameters of specific types are required.
 *
 * <p>This approach is very useful for simple submissions, where binding
 * request parameters to a command object would be overkill.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 23:21
 */
public class RequestContextUtils {

  private static final IntParser INT_PARSER = new IntParser();
  private static final LongParser LONG_PARSER = new LongParser();
  private static final FloatParser FLOAT_PARSER = new FloatParser();
  private static final DoubleParser DOUBLE_PARSER = new DoubleParser();
  private static final BooleanParser BOOLEAN_PARSER = new BooleanParser();
  private static final StringParser STRING_PARSER = new StringParser();

  @Nullable
  public static <T> T getBean(RequestContext request, Class<T> requiredType) {
    ApplicationContext beanFactory = request.getApplicationContext();
    if (beanFactory != null) {
      return BeanFactoryUtils.find(beanFactory, requiredType);
    }
    return null;
  }

  @Nullable
  public static <T> T getBean(RequestContext request, String beanName) {
    ApplicationContext beanFactory = request.getApplicationContext();
    if (beanFactory != null) {
      return BeanFactoryUtils.find(beanFactory, beanName);
    }
    return null;
  }

  @Nullable
  public static <T> T getBean(RequestContext request, String beanName, Class<T> requiredType) {
    ApplicationContext beanFactory = request.getApplicationContext();
    if (beanFactory != null) {
      return BeanFactoryUtils.find(beanFactory, beanName, requiredType);
    }
    return null;
  }

  /**
   * Determine the session id of the given request, if any.
   *
   * @param request current HTTP request
   * @return the session id, or {@code null} if none
   */
  @Nullable
  public static String getSessionId(RequestContext request) {
    WebSession session = getSession(request, false);
    return session != null ? session.getId() : null;
  }

  /**
   * Returns the current session associated with this request, or if the request
   * does not have a session, creates one.
   *
   * @param request Current request
   * @return the <code>WebSession</code> associated with this request
   * @see #getSession(RequestContext, boolean)
   */
  @Nullable
  public static WebSession getSession(RequestContext request) {
    SessionManager sessionManager = getSessionManager(request);
    if (sessionManager != null) {
      return sessionManager.getSession(request);
    }
    return null;
  }

  /**
   * Returns the current session associated with this request, or if the request
   * does not have a session, creates one.
   *
   * @param request Current request
   * @return the <code>WebSession</code> associated with this request
   * @see #getSession(RequestContext, boolean)
   * @see IllegalStateException
   */
  public static WebSession getRequiredSession(RequestContext request) {
    WebSession session = getSession(request);
    if (session == null) {
      throw new IllegalStateException("Cannot get WebSession");
    }
    return session;
  }

  /**
   * Returns the current <code>WebSession</code> associated with this request or,
   * if there is no current session and <code>create</code> is true, returns a new
   * session.
   *
   * <p>
   * If <code>create</code> is <code>false</code> and the request has no valid
   * <code>WebSession</code>, this method returns <code>null</code>.
   *
   * <p>
   * To make sure the session is properly maintained, you must call this method
   * before the response is committed. If the container is using cookies to
   * maintain session integrity and is asked to create a new session when the
   * response is committed, an IllegalStateException is thrown.
   *
   * @param request Current request
   * @param create <code>true</code> to create a new session for this request if
   * necessary; <code>false</code> to return <code>null</code> if
   * there's no current session
   * @return the <code>WebSession</code> associated with this request or
   * <code>null</code> if <code>create</code> is <code>false</code> and
   * the request has no valid session
   * @see #getSession(RequestContext)
   */
  @Nullable
  public static WebSession getSession(RequestContext request, boolean create) {
    SessionManager sessionManager = getSessionManager(request);
    if (sessionManager != null) {
      return sessionManager.getSession(request, create);
    }
    return null;
  }

  /**
   * Return the WebSessionManager
   *
   * @param request current HTTP request
   * @return the current LocaleResolver, or {@code null} if not found
   */
  @Nullable
  public static SessionManager getSessionManager(RequestContext request) {
    return getBean(request, SessionManager.BEAN_NAME, SessionManager.class);
  }

  /**
   * Return the LocaleResolver that has been bound to the request by the
   * RequestContext.
   *
   * @param request current HTTP request
   * @return the current LocaleResolver, or {@code null} if not found
   */
  @Nullable
  public static LocaleResolver getLocaleResolver(RequestContext request) {
    return getBean(request, LocaleResolver.BEAN_NAME, LocaleResolver.class);
  }

  /**
   * Retrieve the current locale from the given request, using the
   * LocaleResolver bound to the request by the DispatcherServlet
   * (if available), falling back to the request's accept-header Locale.
   * <p>This method serves as a straightforward alternative to the standard
   * Servlet {@link jakarta.servlet.http.HttpServletRequest#getLocale()} method,
   * falling back to the latter if no more specific locale has been found.
   * <p>Consider using {@link LocaleContextHolder#getLocale()}
   * which will normally be populated with the same Locale.
   *
   * @param request current HTTP request
   * @return the current locale for the given request, either from the
   * LocaleResolver or from the plain request itself
   * @see #getLocaleResolver
   * @see LocaleContextHolder#getLocale()
   */
  public static Locale getLocale(RequestContext request) {
    LocaleResolver localeResolver = getLocaleResolver(request);
    return localeResolver != null ? localeResolver.resolveLocale(request) : request.getLocale();
  }

  /**
   * Retrieve the current time zone from the given request, using the
   * {@link TimeZoneAwareLocaleContext} in the {@link LocaleResolver} bound to
   * the request by the {@link DispatcherHandler} (if available).
   * <p>Note: This method returns {@code null} if no specific time zone can be
   * resolved for the given request. This is in contrast to {@link #getLocale}
   * where it is always possible to fall back to the request's locale based on the
   * {@code Accept-Language} header or the default locale for the server.
   * <p>Consider using {@link LocaleContextHolder#getTimeZone()}
   * which will normally be populated with the same {@code TimeZone}: that method only
   * differs in terms of its fallback to the system time zone if the {@code LocaleResolver}
   * hasn't provided a specific time zone (instead of this method's {@code null}).
   *
   * @param request current HTTP request
   * @return the current time zone for the given request, either from the
   * {@code TimeZoneAwareLocaleContext} or {@code null} if none associated
   * @see #getLocaleResolver
   * @see LocaleContextHolder#getTimeZone()
   */
  @Nullable
  public static TimeZone getTimeZone(RequestContext request) {
    LocaleResolver localeResolver = getLocaleResolver(request);
    if (localeResolver instanceof LocaleContextResolver lcr) {
      LocaleContext localeContext = lcr.resolveLocaleContext(request);
      if (localeContext instanceof TimeZoneAwareLocaleContext) {
        return ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
      }
    }
    return null;
  }

  /**
   * Return "output" RedirectModel to save attributes for request after redirect.
   *
   * @param request current request
   * @return a {@link RedirectModel} instance
   */
  @Nullable
  public static RedirectModel getOutputRedirectModel(RequestContext request) {
    return RedirectModel.findOutputModel(request);
  }

  /**
   * Return the {@code RedirectModelManager} instance to save flash attributes.
   * <p>the convenience method {@link #saveRedirectModel} may be
   * used to save the "output" RedirectModel.
   *
   * @param request the current request
   * @return a {@link RedirectModelManager} instance
   */
  @Nullable
  public static RedirectModelManager getRedirectModelManager(RequestContext request) {
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
  public static void saveRedirectModel(String location, RequestContext request) {
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
  public static void saveRedirectModel(
          String location, RequestContext request, @Nullable RedirectModelManager manager) {
    RedirectModel redirectModel = getOutputRedirectModel(request);
    if (CollectionUtils.isNotEmpty(redirectModel)) {
      if (manager == null) {
        manager = getRedirectModelManager(request);
      }
      if (manager != null) {
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(location).build();
        redirectModel.setTargetRequestPath(uriComponents.getPath());
        redirectModel.addTargetRequestParams(uriComponents.getQueryParams());

        manager.saveRedirectModel(request, redirectModel);
      }
    }
  }

  // parameters

  // static
  public static void parseParameters(MultiValueMap<String, String> parameterMap, String s) {
    if (StringUtils.isNotEmpty(s)) {
      int nameStart = 0;
      int valueStart = -1;
      int i;
      int len = s.length();
      loop:
      for (i = 0; i < len; i++) {
        switch (s.charAt(i)) {
          case '=':
            if (nameStart == i) {
              nameStart = i + 1;
            }
            else if (valueStart < nameStart) {
              valueStart = i + 1;
            }
            break;
          case '&':
          case ';':
            addParam(s, nameStart, valueStart, i, parameterMap);
            nameStart = i + 1;
            break;
          case '#':
            break loop;
          default:
            // continue
        }
      }
      addParam(s, nameStart, valueStart, i, parameterMap);
    }
  }

  private static void addParam(String s, int nameStart,
          int valueStart, int valueEnd, MultiValueMap<String, String> params) //
  {

    if (nameStart < valueEnd) {
      if (valueStart <= nameStart) {
        valueStart = valueEnd + 1;
      }
      String name = s.substring(nameStart, valueStart - 1);
      if (valueStart >= valueEnd) {
        // ?name&name1=
        params.add(name, "");
      }
      else {
        String value = s.substring(valueStart, valueEnd);
        params.add(name, value);
      }
    }
  }

  /**
   * Register web-specific scopes ("request", "session")
   * with the given BeanFactory, as used by the ApplicationContext.
   *
   * @param beanFactory the BeanFactory to configure
   */
  public static void registerScopes(ConfigurableBeanFactory beanFactory) {

    // @Autowired WebSession currentSession;
    beanFactory.registerResolvableDependency(WebSession.class, new WebSessionProvider(beanFactory));

    beanFactory.registerScope(RequestContext.SCOPE_REQUEST, RequestScope.instance);
    beanFactory.registerScope(RequestContext.SCOPE_SESSION, new SessionScope(beanFactory));

    // register RequestContext
    // @Autowired RequestContext currentRequest;
    beanFactory.registerResolvableDependency(RequestContext.class, new InjectableRequestContext());
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
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  @Nullable
  public static Integer getIntParameter(RequestContext request, String name)
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
  public static int getIntParameter(RequestContext request, String name, int defaultVal) {
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
  public static int[] getIntParameters(RequestContext request, String name) {
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
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  public static int getRequiredIntParameter(RequestContext request, String name)
          throws RequestBindingException {

    return INT_PARSER.parseInt(name, request.getParameter(name));
  }

  /**
   * Get an array of int parameters, throwing an exception if not found or one is not a number..
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  public static int[] getRequiredIntParameters(RequestContext request, String name)
          throws RequestBindingException {

    return INT_PARSER.parseInts(name, request.getParameters(name));
  }

  /**
   * Get a Long parameter, or {@code null} if not present.
   * Throws an exception if it the parameter value isn't a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @return the Long value, or {@code null} if not present
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  @Nullable
  public static Long getLongParameter(RequestContext request, String name)
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
  public static long getLongParameter(RequestContext request, String name, long defaultVal) {
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
  public static long[] getLongParameters(RequestContext request, String name) {
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
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  public static long getRequiredLongParameter(RequestContext request, String name)
          throws RequestBindingException {

    return LONG_PARSER.parseLong(name, request.getParameter(name));
  }

  /**
   * Get an array of long parameters, throwing an exception if not found or one is not a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  public static long[] getRequiredLongParameters(RequestContext request, String name)
          throws RequestBindingException {

    return LONG_PARSER.parseLongs(name, request.getParameters(name));
  }

  /**
   * Get a Float parameter, or {@code null} if not present.
   * Throws an exception if it the parameter value isn't a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @return the Float value, or {@code null} if not present
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  @Nullable
  public static Float getFloatParameter(RequestContext request, String name)
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
  public static float getFloatParameter(RequestContext request, String name, float defaultVal) {
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
  public static float[] getFloatParameters(RequestContext request, String name) {
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
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  public static float getRequiredFloatParameter(RequestContext request, String name)
          throws RequestBindingException {

    return FLOAT_PARSER.parseFloat(name, request.getParameter(name));
  }

  /**
   * Get an array of float parameters, throwing an exception if not found or one is not a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  public static float[] getRequiredFloatParameters(RequestContext request, String name)
          throws RequestBindingException {

    return FLOAT_PARSER.parseFloats(name, request.getParameters(name));
  }

  /**
   * Get a Double parameter, or {@code null} if not present.
   * Throws an exception if it the parameter value isn't a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @return the Double value, or {@code null} if not present
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  @Nullable
  public static Double getDoubleParameter(RequestContext request, String name)
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
  public static double getDoubleParameter(RequestContext request, String name, double defaultVal) {
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
  public static double[] getDoubleParameters(RequestContext request, String name) {
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
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  public static double getRequiredDoubleParameter(RequestContext request, String name)
          throws RequestBindingException {

    return DOUBLE_PARSER.parseDouble(name, request.getParameter(name));
  }

  /**
   * Get an array of double parameters, throwing an exception if not found or one is not a number.
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  public static double[] getRequiredDoubleParameters(RequestContext request, String name)
          throws RequestBindingException {

    return DOUBLE_PARSER.parseDoubles(name, request.getParameters(name));
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
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  @Nullable
  public static Boolean getBooleanParameter(RequestContext request, String name)
          throws RequestBindingException {

    if (request.getParameter(name) == null) {
      return null;
    }
    return (getRequiredBooleanParameter(request, name));
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
  public static boolean getBooleanParameter(RequestContext request, String name, boolean defaultVal) {
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
  public static boolean[] getBooleanParameters(RequestContext request, String name) {
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
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  public static boolean getRequiredBooleanParameter(RequestContext request, String name)
          throws RequestBindingException {

    return BOOLEAN_PARSER.parseBoolean(name, request.getParameter(name));
  }

  /**
   * Get an array of boolean parameters, throwing an exception if not found
   * or one isn't a boolean.
   * <p>Accepts "true", "on", "yes" (any case) and "1" as values for true;
   * treats every other non-empty value as false (i.e. parses leniently).
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  public static boolean[] getRequiredBooleanParameters(RequestContext request, String name)
          throws RequestBindingException {
    return BOOLEAN_PARSER.parseBooleans(name, request.getParameters(name));
  }

  /**
   * Get a String parameter, or {@code null} if not present.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @return the String value, or {@code null} if not present
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  @Nullable
  public static String getStringParameter(RequestContext request, String name)
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
  public static String getStringParameter(RequestContext request, String name, String defaultVal) {
    String val = request.getParameter(name);
    return (val != null ? val : defaultVal);
  }

  /**
   * Get an array of String parameters, return an empty array if not found.
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   */
  public static String[] getStringParameters(RequestContext request, String name) {
    try {
      return getRequiredStringParameters(request, name);
    }
    catch (RequestBindingException ex) {
      return new String[0];
    }
  }

  /**
   * Get a String parameter, throwing an exception if it isn't found.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  public static String getRequiredStringParameter(RequestContext request, String name)
          throws RequestBindingException {

    return STRING_PARSER.validateRequiredString(name, request.getParameter(name));
  }

  /**
   * Get an array of String parameters, throwing an exception if not found.
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @throws RequestBindingException a subclass of ServletException,
   * so it doesn't need to be caught
   */
  public static String[] getRequiredStringParameters(RequestContext request, String name)
          throws RequestBindingException {

    return STRING_PARSER.validateRequiredStrings(name, request.getParameters(name));
  }

  private abstract static class ParameterParser<T> {

    protected final T parse(String name, String parameter) throws RequestBindingException {
      validateRequiredParameter(name, parameter);
      try {
        return doParse(parameter);
      }
      catch (NumberFormatException ex) {
        throw new RequestBindingException(
                "Required " + getType() + " parameter '" + name + "' with value of '" +
                        parameter + "' is not a valid number", ex);
      }
    }

    protected final void validateRequiredParameter(String name, @Nullable Object parameter)
            throws RequestBindingException {

      if (parameter == null) {
        throw new MissingRequestParameterException(name, getType());
      }
    }

    protected abstract String getType();

    protected abstract T doParse(String parameter) throws NumberFormatException;
  }

  private static class IntParser extends ParameterParser<Integer> {

    @Override
    protected String getType() {
      return "int";
    }

    @Override
    protected Integer doParse(String s) throws NumberFormatException {
      return Integer.valueOf(s);
    }

    public int parseInt(String name, String parameter) throws RequestBindingException {
      return parse(name, parameter);
    }

    public int[] parseInts(String name, String[] values) throws RequestBindingException {
      validateRequiredParameter(name, values);
      int[] parameters = new int[values.length];
      for (int i = 0; i < values.length; i++) {
        parameters[i] = parseInt(name, values[i]);
      }
      return parameters;
    }
  }

  private static class LongParser extends ParameterParser<Long> {

    @Override
    protected String getType() {
      return "long";
    }

    @Override
    protected Long doParse(String parameter) throws NumberFormatException {
      return Long.valueOf(parameter);
    }

    public long parseLong(String name, String parameter) throws RequestBindingException {
      return parse(name, parameter);
    }

    public long[] parseLongs(String name, String[] values) throws RequestBindingException {
      validateRequiredParameter(name, values);
      long[] parameters = new long[values.length];
      for (int i = 0; i < values.length; i++) {
        parameters[i] = parseLong(name, values[i]);
      }
      return parameters;
    }
  }

  private static class FloatParser extends ParameterParser<Float> {

    @Override
    protected String getType() {
      return "float";
    }

    @Override
    protected Float doParse(String parameter) throws NumberFormatException {
      return Float.valueOf(parameter);
    }

    public float parseFloat(String name, String parameter) throws RequestBindingException {
      return parse(name, parameter);
    }

    public float[] parseFloats(String name, String[] values) throws RequestBindingException {
      validateRequiredParameter(name, values);
      float[] parameters = new float[values.length];
      for (int i = 0; i < values.length; i++) {
        parameters[i] = parseFloat(name, values[i]);
      }
      return parameters;
    }
  }

  private static class DoubleParser extends ParameterParser<Double> {

    @Override
    protected String getType() {
      return "double";
    }

    @Override
    protected Double doParse(String parameter) throws NumberFormatException {
      return Double.valueOf(parameter);
    }

    public double parseDouble(String name, String parameter) throws RequestBindingException {
      return parse(name, parameter);
    }

    public double[] parseDoubles(String name, String[] values) throws RequestBindingException {
      validateRequiredParameter(name, values);
      double[] parameters = new double[values.length];
      for (int i = 0; i < values.length; i++) {
        parameters[i] = parseDouble(name, values[i]);
      }
      return parameters;
    }
  }

  private static class BooleanParser extends ParameterParser<Boolean> {

    @Override
    protected String getType() {
      return "boolean";
    }

    @Override
    protected Boolean doParse(String parameter) throws NumberFormatException {
      return (parameter.equalsIgnoreCase("true") || parameter.equalsIgnoreCase("on") ||
              parameter.equalsIgnoreCase("yes") || parameter.equals("1"));
    }

    public boolean parseBoolean(String name, String parameter) throws RequestBindingException {
      return parse(name, parameter);
    }

    public boolean[] parseBooleans(String name, String[] values) throws RequestBindingException {
      validateRequiredParameter(name, values);
      boolean[] parameters = new boolean[values.length];
      for (int i = 0; i < values.length; i++) {
        parameters[i] = parseBoolean(name, values[i]);
      }
      return parameters;
    }
  }

  private static class StringParser extends ParameterParser<String> {

    @Override
    protected String getType() {
      return "string";
    }

    @Override
    protected String doParse(String parameter) throws NumberFormatException {
      return parameter;
    }

    public String validateRequiredString(String name, String value) throws RequestBindingException {
      validateRequiredParameter(name, value);
      return value;
    }

    public String[] validateRequiredStrings(String name, String[] values) throws RequestBindingException {
      validateRequiredParameter(name, values);
      for (String value : values) {
        validateRequiredParameter(name, value);
      }
      return values;
    }
  }

  //

  /**
   * Factory that exposes the current web-session object on demand.
   */
  final static class WebSessionProvider implements Supplier<WebSession>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final SessionManagerDiscover sessionManagerDiscover;

    private WebSessionProvider(BeanFactory beanFactory) {
      this.sessionManagerDiscover = new SessionManagerDiscover(beanFactory);
    }

    @Override
    public WebSession get() {
      RequestContext request = RequestContextHolder.getRequired();
      return sessionManagerDiscover.obtain(request)
              .getSession(request);
    }

    @Override
    public String toString() {
      return "Current WebSession";
    }

  }

  /**
   * Factory that exposes the current request-context object on demand.
   */
  private static class InjectableRequestContext extends DecoratingRequestContext {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public RequestContext getDelegate() {
      return RequestContextHolder.getRequired();
    }

    @Override
    public String toString() {
      return "Current RequestContext";
    }

  }

}
