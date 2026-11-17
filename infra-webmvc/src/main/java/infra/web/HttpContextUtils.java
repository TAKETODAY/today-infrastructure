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

package infra.web;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Supplier;

import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ApplicationContext;
import infra.core.i18n.LocaleContext;
import infra.core.i18n.LocaleContextHolder;
import infra.core.i18n.TimeZoneAwareLocaleContext;
import infra.lang.Constant;
import infra.lang.Contract;
import infra.session.Session;
import infra.util.CollectionUtils;
import infra.web.bind.MissingRequestParameterException;
import infra.web.bind.RequestBindingException;
import infra.web.context.support.RequestScope;
import infra.web.context.support.SessionScope;
import infra.web.util.UriComponents;
import infra.web.util.UriComponentsBuilder;

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
public final class HttpContextUtils {

  private HttpContextUtils() {
  }

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
   * @see HttpContext#getApplicationContext()
   * @see BeanFactoryUtils#find(infra.beans.factory.BeanFactory, String, Class)
   */
  public static <T> @Nullable T getBean(HttpContext request, String beanName, Class<T> requiredType) {
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
  public static @Nullable LocaleResolver getLocaleResolver(HttpContext request) {
    return getBean(request, LocaleResolver.BEAN_NAME, LocaleResolver.class);
  }

  /**
   * Resolve the effective locale for application use, such as message lookup,
   * view rendering, and formatting.
   * <p>If a configured {@link #getLocaleResolver LocaleResolver} is available,
   * return its result directly: the resolver is responsible for its own defaults
   * and must return a non-null locale. Only when no resolver is available, use
   * {@link HttpContext#getLocale()} and, if that is null, {@link Locale#getDefault()}.
   * <p>Unlike {@link HttpContext#getLocale()}, this method always returns
   * a non-null locale on successful resolution. It does not suppress parsing
   * or resolver exceptions, or restrict the result to supported languages.
   * Configure application defaults and supported languages on the resolver.
   * <p>Resolver implementations must not call this method to obtain a fallback,
   * since doing so may invoke the same resolver recursively. They should read
   * {@link HttpContext#getLocale()} directly instead.
   * <p>Consider using {@link LocaleContextHolder#getLocale()}
   * which will normally be populated with the same Locale.
   *
   * @param request current HTTP request
   * @return the effective locale (never {@code null})
   * @see #getLocaleResolver
   * @see LocaleContextHolder#getLocale()
   */
  public static Locale resolveLocale(HttpContext request) {
    LocaleResolver localeResolver = getLocaleResolver(request);
    if (localeResolver != null) {
      return localeResolver.resolveLocale(request);
    }
    Locale locale = request.getLocale();
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
   * @param request current HTTP request
   * @return the current time zone for the given request, either from the
   * {@code TimeZoneAwareLocaleContext} or {@code null} if none associated
   * @see #getLocaleResolver
   * @see LocaleContextHolder#getTimeZone()
   */
  public static @Nullable TimeZone getTimeZone(HttpContext request) {
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
  public static @Nullable RedirectModel getOutputRedirectModel(HttpContext request) {
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
  public static @Nullable RedirectModelManager getRedirectModelManager(HttpContext request) {
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
  public static void saveRedirectModel(String location, HttpContext request) {
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
  public static void saveRedirectModel(String location, HttpContext request, @Nullable RedirectModelManager manager) {
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

  // static

  /**
   * Register web-specific scopes ("request", "session")
   * with the given BeanFactory, as used by the ApplicationContext.
   *
   * @param beanFactory the BeanFactory to configure
   */
  public static void registerScopes(ConfigurableBeanFactory beanFactory) {

    // @Autowired Session currentSession;
    beanFactory.registerResolvableDependency(Session.class, new WebSessionProvider());

    beanFactory.registerScope(HttpContext.SCOPE_REQUEST, RequestScope.instance);
    beanFactory.registerScope(HttpContext.SCOPE_SESSION, new SessionScope());

    // register HttpContext
    // @Autowired HttpContext currentRequest;
    beanFactory.registerResolvableDependency(HttpContext.class, new InjectableHttpContext());
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
  public static @Nullable Integer getIntParameter(HttpContext request, String name)
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
  public static int getIntParameter(HttpContext request, String name, int defaultVal) {
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
  public static int[] getIntParameters(HttpContext request, String name) {
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
  public static int getRequiredIntParameter(HttpContext request, String name)
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
  public static int[] getRequiredIntParameters(HttpContext request, String name)
          throws RequestBindingException {

    return IntParser.INSTANCE.parseInts(name, request.getParameters(name));
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
  public static @Nullable Long getLongParameter(HttpContext request, String name)
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
  public static long getLongParameter(HttpContext request, String name, long defaultVal) {
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
  public static long[] getLongParameters(HttpContext request, String name) {
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
  public static long getRequiredLongParameter(HttpContext request, String name)
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
  public static long[] getRequiredLongParameters(HttpContext request, String name)
          throws RequestBindingException {

    return LongParser.INSTANCE.parseLongs(name, request.getParameters(name));
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
  public static @Nullable Float getFloatParameter(HttpContext request, String name)
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
  public static float getFloatParameter(HttpContext request, String name, float defaultVal) {
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
  public static float[] getFloatParameters(HttpContext request, String name) {
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
  public static float getRequiredFloatParameter(HttpContext request, String name)
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
  public static float[] getRequiredFloatParameters(HttpContext request, String name)
          throws RequestBindingException {

    return FloatParser.INSTANCE.parseFloats(name, request.getParameters(name));
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
  public static @Nullable Double getDoubleParameter(HttpContext request, String name)
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
  public static double getDoubleParameter(HttpContext request, String name, double defaultVal) {
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
  public static double[] getDoubleParameters(HttpContext request, String name) {
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
  public static double getRequiredDoubleParameter(HttpContext request, String name)
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
  public static double[] getRequiredDoubleParameters(HttpContext request, String name)
          throws RequestBindingException {

    return DoubleParser.INSTANCE.parseDoubles(name, request.getParameters(name));
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
  public static @Nullable Boolean getBooleanParameter(HttpContext request, String name)
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
  public static boolean getBooleanParameter(HttpContext request, String name, boolean defaultVal) {
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
  public static boolean[] getBooleanParameters(HttpContext request, String name) {
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
  public static boolean getRequiredBooleanParameter(HttpContext request, String name)
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
  public static boolean[] getRequiredBooleanParameters(HttpContext request, String name)
          throws RequestBindingException {
    return BooleanParser.INSTANCE.parseBooleans(name, request.getParameters(name));
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
  public static @Nullable String getStringParameter(HttpContext request, String name)
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
  public static String getStringParameter(HttpContext request, String name, String defaultVal) {
    String val = request.getParameter(name);
    return (val != null ? val : defaultVal);
  }

  /**
   * Get an array of String parameters, return an empty array if not found.
   *
   * @param request current HTTP request
   * @param name the name of the parameter with multiple possible values
   */
  public static String[] getStringParameters(HttpContext request, String name) {
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
  public static String getRequiredStringParameter(HttpContext request, String name)
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
  public static String[] getRequiredStringParameters(HttpContext request, String name)
          throws RequestBindingException {

    return StringParser.INSTANCE.validateRequiredStrings(name, request.getParameters(name));
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

  //

  /**
   * Factory that exposes the current web-session object on demand.
   */
  static final class WebSessionProvider implements Supplier<Session>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public Session get() {
      return HttpContextHolder.required().getSession();
    }

    @Override
    public String toString() {
      return "Current Session";
    }

  }

  /**
   * Factory that exposes the current request-context object on demand.
   */
  private static final class InjectableHttpContext extends AbstractDelegatingHttpContext {

    @Override
    public HttpContext delegate() {
      return HttpContextHolder.required();
    }

    @Override
    public String toString() {
      return "Current HttpContext";
    }

  }

}
