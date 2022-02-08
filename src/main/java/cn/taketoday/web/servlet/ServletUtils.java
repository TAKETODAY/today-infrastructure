/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import cn.taketoday.core.Conventions;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.util.WebUtils;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ServletResponseWrapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * @author TODAY 2020/12/8 23:07
 * @since 3.0
 */
public abstract class ServletUtils {

  /** Key for the mutex session attribute. */
  public static final String SESSION_MUTEX_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          ServletUtils.class, "MUTEX");

  /**
   * Request attribute to hold the current web application context.
   * Otherwise only the global web app context is obtainable by tags etc.
   */
  public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          ServletUtils.class, "CONTEXT");

  /** Name suffixes in case of image buttons.  @since 4.0 */
  public static final String[] SUBMIT_IMAGE_SUFFIXES = { ".x", ".y" };

  /**
   * Standard Servlet spec context attribute that specifies a temporary
   * directory for the current web application, of type {@code java.io.File}.
   *
   * @since 4.0
   */
  public static final String TEMP_DIR_CONTEXT_ATTRIBUTE = "jakarta.servlet.context.tempdir";

  /**
   * HTML escape parameter at the servlet context level
   * (i.e. a context-param in {@code web.xml}): "defaultHtmlEscape".
   *
   * @since 4.0
   */
  public static final String HTML_ESCAPE_CONTEXT_PARAM = "defaultHtmlEscape";

  /**
   * Use of response encoding for HTML escaping parameter at the servlet context level
   * (i.e. a context-param in {@code web.xml}): "responseEncodedHtmlEscape".
   *
   * @since 4.0
   */
  public static final String RESPONSE_ENCODED_HTML_ESCAPE_CONTEXT_PARAM = "responseEncodedHtmlEscape";

  /**
   * Web app root key parameter at the servlet context level
   * (i.e. a context-param in {@code web.xml}): "webAppRootKey".
   *
   * @since 4.0
   */
  public static final String WEB_APP_ROOT_KEY_PARAM = "webAppRootKey";

  /**
   * Default web app root key: "webapp.root".
   *
   * @since 4.0
   */
  public static final String DEFAULT_WEB_APP_ROOT_KEY = "webapp.root";

  // context

  public static RequestContext getRequestContext(ServletRequest request, ServletResponse response) {
    HttpServletRequest servletRequest = (HttpServletRequest) request;
    return getRequestContext(findWebApplicationContext(servletRequest), servletRequest, (HttpServletResponse) response);
  }

  public static RequestContext getRequestContext(
          WebApplicationContext webApplicationContext, HttpServletRequest request, HttpServletResponse response) {
    return new ServletRequestContext(webApplicationContext, request, response);
  }

  /**
   * Get HttpSession
   *
   * @throws IllegalStateException Not run in servlet
   */
  public static HttpSession getHttpSession(final RequestContext context) {
    return getHttpSession(context, true);
  }

  /**
   * Returns the current <code>HttpSession</code>
   * associated with this request or, if there is no
   * current session and <code>create</code> is true, returns
   * a new session.
   *
   * <p>If <code>create</code> is <code>false</code>
   * and the request has no valid <code>HttpSession</code>,
   * this method returns <code>null</code>.
   *
   * <p>To make sure the session is properly maintained,
   * you must call this method before
   * the response is committed. If the container is using cookies
   * to maintain session integrity and is asked to create a new session
   * when the response is committed, an IllegalStateException is thrown.
   *
   * @param create <code>true</code> to create
   * a new session for this request if necessary;
   * <code>false</code> to return <code>null</code>
   * if there's no current session
   * @return the <code>HttpSession</code> associated
   * with this request or <code>null</code> if
   * <code>create</code> is <code>false</code>
   * and the request has no valid session
   * @throws IllegalStateException Not run in servlet
   * @see #getHttpSession(RequestContext)
   */
  public static HttpSession getHttpSession(final RequestContext context, boolean create) {
    if (context instanceof ServletRequestContext) {
      final HttpServletRequest request = ((ServletRequestContext) context).getRequest();
      return request.getSession(create);
    }
    throw new IllegalStateException("Not run in servlet");
  }

  /**
   * Return an appropriate request object of the specified type, if available,
   * unwrapping the given request as far as necessary.
   *
   * @param request the servlet request to introspect
   * @param requiredType the desired type of request object
   * @return the matching request object, or {@code null} if none
   * of that type is available
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public static <T> T getNativeRequest(ServletRequest request, @Nullable Class<T> requiredType) {
    if (requiredType != null) {
      if (requiredType.isInstance(request)) {
        return (T) request;
      }
      else if (request instanceof ServletRequestWrapper wrapper) {
        return getNativeRequest(wrapper.getRequest(), requiredType);
      }
    }
    return null;
  }

  /**
   * Return an appropriate response object of the specified type, if available,
   * unwrapping the given response as far as necessary.
   *
   * @param response the servlet response to introspect
   * @param requiredType the desired type of response object
   * @return the matching response object, or {@code null} if none
   * of that type is available
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public static <T> T getNativeResponse(ServletResponse response, @Nullable Class<T> requiredType) {
    if (requiredType != null) {
      if (requiredType.isInstance(response)) {
        return (T) response;
      }
      else if (response instanceof ServletResponseWrapper wrapper) {
        return getNativeResponse(wrapper.getResponse(), requiredType);
      }
    }
    return null;
  }

  /**
   * Return an appropriate HttpServletRequest object
   *
   * @param context the context to introspect
   * @return the matching request object
   * @see WebUtils#getNativeContext(RequestContext, Class)
   */
  public static HttpServletRequest getServletRequest(RequestContext context) {
    ServletRequestContext nativeContext = WebUtils.getNativeContext(context, ServletRequestContext.class);
    Assert.state(nativeContext != null, "Not run in servlet");
    return nativeContext.getRequest();
  }

  /**
   * Return an appropriate response object
   *
   * @param context the context to introspect
   * @return the matching response object
   * @see WebUtils#getNativeContext(RequestContext, Class)
   */
  public static HttpServletResponse getServletResponse(RequestContext context) {
    ServletRequestContext nativeContext = WebUtils.getNativeContext(context, ServletRequestContext.class);
    Assert.state(nativeContext != null, "Not run in servlet");
    return nativeContext.getResponse();
  }

  /**
   * Look for the WebApplicationContext associated with the DispatcherServlet
   * that has initiated request processing, and for the global context if none
   * was found associated with the current request. The global context will
   * be found via the ServletContext or via ContextLoader's current context.
   *
   * @param request current HTTP request
   * @return the request-specific WebApplicationContext, or the global one
   * if no request-specific context has been found, or {@code null} if none
   * @see #findWebApplicationContext(ServletRequest, ServletContext)
   * @see ServletRequest#getServletContext()
   * @since 4.0
   */
  @Nullable
  public static WebApplicationContext findWebApplicationContext(ServletRequest request) {
    return findWebApplicationContext(request, request.getServletContext());
  }

  /**
   * Look for the WebApplicationContext associated with the DispatcherServlet
   * that has initiated request processing, and for the global context if none
   * was found associated with the current request. The global context will
   * be found via the ServletContext or via ContextLoader's current context.
   * <p>NOTE: This variant remains compatible with Servlet 2.5, explicitly
   * checking a given ServletContext instead of deriving it from the request.
   *
   * @param request current HTTP request
   * @param servletContext current servlet context
   * @return the request-specific WebApplicationContext, or the global one
   * if no request-specific context has been found, or {@code null} if none
   * @see #WEB_APPLICATION_CONTEXT_ATTRIBUTE
   * @see #getWebApplicationContext(ServletContext)
   * @since 4.0
   */
  @Nullable
  public static WebApplicationContext findWebApplicationContext(
          ServletRequest request, @Nullable ServletContext servletContext) {
    WebApplicationContext webApplicationContext = (WebApplicationContext) request.getAttribute(
            WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    if (webApplicationContext == null) {
      if (servletContext != null) {
        webApplicationContext = findWebApplicationContext(servletContext);
      }
    }
    return webApplicationContext;
  }

  /**
   * Find a unique {@code WebApplicationContext} for this web app: either the
   * root web app context (preferred) or a unique {@code WebApplicationContext}
   * among the registered {@code ServletContext} attributes (typically coming
   * from a single {@code DispatcherServlet} in the current web application).
   * <p>Note that {@code DispatcherServlet}'s exposure of its context can be
   * controlled through its {@code publishContext} property, which is {@code true}
   * by default but can be selectively switched to only publish a single context
   * despite multiple {@code DispatcherServlet} registrations in the web app.
   *
   * @param sc the ServletContext to find the web application context for
   * @return the desired WebApplicationContext for this web app, or {@code null} if none
   * @see #getWebApplicationContext(ServletContext)
   * @see ServletContext#getAttributeNames()
   * @since 4.0
   */
  @Nullable
  public static WebApplicationContext findWebApplicationContext(ServletContext sc) {
    WebApplicationContext wac = getWebApplicationContext(sc);
    if (wac == null) {
      Enumeration<String> attrNames = sc.getAttributeNames();
      while (attrNames.hasMoreElements()) {
        String attrName = attrNames.nextElement();
        Object attrValue = sc.getAttribute(attrName);
        if (attrValue instanceof WebApplicationContext) {
          if (wac != null) {
            throw new IllegalStateException("No unique WebApplicationContext found: more than one " +
                    "DispatcherServlet registered with publishContext=true?");
          }
          wac = (WebApplicationContext) attrValue;
        }
      }
    }
    return wac;
  }

  /**
   * Find the root {@code WebApplicationContext} for this web app,
   *
   * @param sc the ServletContext to find the web application context for
   * @since 4.0
   */
  @Nullable
  public static WebApplicationContext getWebApplicationContext(ServletContext sc) {
    return (WebApplicationContext) sc.getAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE);
  }

  //---------------------------------------------------------------------
  // HttpSession
  //---------------------------------------------------------------------

  /**
   * Determine the session id of the given request, if any.
   *
   * @param request current HTTP request
   * @return the session id, or {@code null} if none
   * @since 4.0
   */
  @Nullable
  public static String getSessionId(HttpServletRequest request) {
    Assert.notNull(request, "Request must not be null");
    HttpSession session = request.getSession(false);
    return (session != null ? session.getId() : null);
  }

  /**
   * Check the given request for a session attribute of the given name.
   * Returns null if there is no session or if the session has no such attribute.
   * Does not create a new session if none has existed before!
   *
   * @param request current HTTP request
   * @param name the name of the session attribute
   * @return the value of the session attribute, or {@code null} if not found
   * @since 4.0
   */
  @Nullable
  public static Object getSessionAttribute(HttpServletRequest request, String name) {
    Assert.notNull(request, "Request must not be null");
    HttpSession session = request.getSession(false);
    return session != null ? session.getAttribute(name) : null;
  }

  /**
   * Check the given request for a session attribute of the given name.
   * Throws an exception if there is no session or if the session has no such
   * attribute. Does not create a new session if none has existed before!
   *
   * @param request current HTTP request
   * @param name the name of the session attribute
   * @return the value of the session attribute, or {@code null} if not found
   * @throws IllegalStateException if the session attribute could not be found
   * @since 4.0
   */
  public static Object getRequiredSessionAttribute(HttpServletRequest request, String name) {
    Object attr = getSessionAttribute(request, name);
    if (attr == null) {
      throw new IllegalStateException("No session attribute '" + name + "' found");
    }
    return attr;
  }

  /**
   * Set the session attribute with the given name to the given value.
   * Removes the session attribute if value is null, if a session existed at all.
   * Does not create a new session if not necessary!
   *
   * @param request current HTTP request
   * @param name the name of the session attribute
   * @param value the value of the session attribute
   * @since 4.0
   */
  public static void setSessionAttribute(HttpServletRequest request, String name, @Nullable Object value) {
    Assert.notNull(request, "Request must not be null");
    if (value != null) {
      request.getSession().setAttribute(name, value);
    }
    else {
      HttpSession session = request.getSession(false);
      if (session != null) {
        session.removeAttribute(name);
      }
    }
  }

  /**
   * Return the best available mutex for the given session:
   * that is, an object to synchronize on for the given session.
   * <p>Returns the session mutex attribute if available; usually,
   * this means that the HttpSessionMutexListener needs to be defined
   * in {@code web.xml}. Falls back to the HttpSession itself
   * if no mutex attribute found.
   * <p>The session mutex is guaranteed to be the same object during
   * the entire lifetime of the session, available under the key defined
   * by the {@code SESSION_MUTEX_ATTRIBUTE} constant. It serves as a
   * safe reference to synchronize on for locking on the current session.
   * <p>In many cases, the HttpSession reference itself is a safe mutex
   * as well, since it will always be the same object reference for the
   * same active logical session. However, this is not guaranteed across
   * different servlet containers; the only 100% safe way is a session mutex.
   *
   * @param session the HttpSession to find a mutex for
   * @return the mutex object (never {@code null})
   * @see #SESSION_MUTEX_ATTRIBUTE
   * @since 4.0
   */
  public static Object getSessionMutex(HttpSession session) {
    Assert.notNull(session, "Session must not be null");
    Object mutex = session.getAttribute(SESSION_MUTEX_ATTRIBUTE);
    if (mutex == null) {
      mutex = session;
    }
    return mutex;
  }

  //---------------------------------------------------------------------
  // ServletRequest
  //---------------------------------------------------------------------

  /**
   * Check if a specific input type="submit" parameter was sent in the request,
   * either via a button (directly with name) or via an image (name + ".x" or
   * name + ".y").
   *
   * @param request current HTTP request
   * @param name the name of the parameter
   * @return if the parameter was sent
   * @see #SUBMIT_IMAGE_SUFFIXES
   * @since 4.0
   */
  public static boolean hasSubmitParameter(ServletRequest request, String name) {
    Assert.notNull(request, "Request must not be null");
    if (request.getParameter(name) != null) {
      return true;
    }
    for (String suffix : SUBMIT_IMAGE_SUFFIXES) {
      if (request.getParameter(name + suffix) != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determine whether the given request is an include request,
   * that is, not a top-level HTTP request coming in from the outside.
   * <p>Checks the presence of the "jakarta.servlet.include.request_uri"
   * request attribute. Could check any request attribute that is only
   * present in an include request.
   *
   * @param request current servlet request
   * @return whether the given request is an include request
   * @since 4.0
   */
  public static boolean isIncludeRequest(ServletRequest request) {
    return request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null;
  }

  /**
   * Retrieve the first cookie with the given name. Note that multiple
   * cookies can have the same name but different paths or domains.
   *
   * @param request current servlet request
   * @param name cookie name
   * @return the first cookie with the given name, or {@code null} if none is found
   * @since 4.0
   */
  @Nullable
  public static Cookie getCookie(HttpServletRequest request, String name) {
    Assert.notNull(request, "Request must not be null");
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (name.equals(cookie.getName())) {
          return cookie;
        }
      }
    }
    return null;
  }

  /**
   * Return a map containing all parameters with the given prefix.
   * Maps single values to String and multiple values to String array.
   * <p>For example, with a prefix of "spring_", "spring_param1" and
   * "spring_param2" result in a Map with "param1" and "param2" as keys.
   *
   * @param request the HTTP request in which to look for parameters
   * @param prefix the beginning of parameter names
   * (if this is null or the empty string, all parameters will match)
   * @return map containing request parameters <b>without the prefix</b>,
   * containing either a String or a String array as values
   * @see ServletRequest#getParameterNames
   * @see ServletRequest#getParameterValues
   * @see ServletRequest#getParameterMap
   */
  public static Map<String, Object> getParametersStartingWith(ServletRequest request, @Nullable String prefix) {
    Assert.notNull(request, "Request must not be null");
    Enumeration<String> paramNames = request.getParameterNames();
    Map<String, Object> params = new TreeMap<>();
    if (prefix == null) {
      prefix = "";
    }
    while (paramNames != null && paramNames.hasMoreElements()) {
      String paramName = paramNames.nextElement();
      if (prefix.isEmpty() || paramName.startsWith(prefix)) {
        String unprefixed = paramName.substring(prefix.length());
        String[] values = request.getParameterValues(paramName);
        if (ObjectUtils.isNotEmpty(values)) {
          if (values.length > 1) {
            params.put(unprefixed, values);
          }
          else {
            params.put(unprefixed, values[0]);
          }
        }
        // else Do nothing, no values found at all.
      }
    }
    return params;
  }

  /**
   * Obtain a named parameter from the given request parameters.
   * <p>See {@link #findParameterValue(Map, String)}
   * for a description of the lookup algorithm.
   *
   * @param request current HTTP request
   * @param name the <i>logical</i> name of the request parameter
   * @return the value of the parameter, or {@code null}
   * if the parameter does not exist in given request
   */
  @Nullable
  public static String findParameterValue(ServletRequest request, String name) {
    return findParameterValue(request.getParameterMap(), name);
  }

  /**
   * Obtain a named parameter from the given request parameters.
   * <p>This method will try to obtain a parameter value using the
   * following algorithm:
   * <ol>
   * <li>Try to get the parameter value using just the given <i>logical</i> name.
   * This handles parameters of the form <tt>logicalName = value</tt>. For normal
   * parameters, e.g. submitted using a hidden HTML form field, this will return
   * the requested value.</li>
   * <li>Try to obtain the parameter value from the parameter name, where the
   * parameter name in the request is of the form <tt>logicalName_value = xyz</tt>
   * with "_" being the configured delimiter. This deals with parameter values
   * submitted using an HTML form submit button.</li>
   * <li>If the value obtained in the previous step has a ".x" or ".y" suffix,
   * remove that. This handles cases where the value was submitted using an
   * HTML form image button. In this case the parameter in the request would
   * actually be of the form <tt>logicalName_value.x = 123</tt>. </li>
   * </ol>
   *
   * @param parameters the available parameter map
   * @param name the <i>logical</i> name of the request parameter
   * @return the value of the parameter, or {@code null}
   * if the parameter does not exist in given request
   */
  @Nullable
  public static String findParameterValue(Map<String, ?> parameters, String name) {
    // First try to get it as a normal name=value parameter
    Object value = parameters.get(name);
    if (value instanceof String[] values) {
      return (values.length > 0 ? values[0] : null);
    }
    else if (value != null) {
      return value.toString();
    }
    // If no value yet, try to get it as a name_value=xyz parameter
    String prefix = name + "_";
    for (String paramName : parameters.keySet()) {
      if (paramName.startsWith(prefix)) {
        // Support images buttons, which would submit parameters as name_value.x=123
        for (String suffix : SUBMIT_IMAGE_SUFFIXES) {
          if (paramName.endsWith(suffix)) {
            return paramName.substring(prefix.length(), paramName.length() - suffix.length());
          }
        }
        return paramName.substring(prefix.length());
      }
    }
    // We couldn't find the parameter value...
    return null;
  }

  /**
   * Expose the Servlet spec's error attributes as {@link HttpServletRequest}
   * attributes under the keys defined in the Servlet 2.3 specification, for error pages that
   * are rendered directly rather than through the Servlet container's error page resolution:
   * {@code jakarta.servlet.error.status_code},
   * {@code jakarta.servlet.error.exception_type},
   * {@code jakarta.servlet.error.message},
   * {@code jakarta.servlet.error.exception},
   * {@code jakarta.servlet.error.request_uri},
   * {@code jakarta.servlet.error.servlet_name}.
   * <p>Does not override values if already present, to respect attribute values
   * that have been exposed explicitly before.
   * <p>Exposes status code 200 by default. Set the "jakarta.servlet.error.status_code"
   * attribute explicitly (before or after) in order to expose a different status code.
   *
   * @param request current servlet request
   * @param ex the exception encountered
   * @param servletName the name of the offending servlet
   * @since 4.0
   */
  public static void exposeErrorRequestAttributes(
          HttpServletRequest request, Throwable ex, @Nullable String servletName) {

    exposeRequestAttributeIfNotPresent(request, RequestDispatcher.ERROR_STATUS_CODE, HttpServletResponse.SC_OK);
    exposeRequestAttributeIfNotPresent(request, RequestDispatcher.ERROR_EXCEPTION_TYPE, ex.getClass());
    exposeRequestAttributeIfNotPresent(request, RequestDispatcher.ERROR_MESSAGE, ex.getMessage());
    exposeRequestAttributeIfNotPresent(request, RequestDispatcher.ERROR_EXCEPTION, ex);
    exposeRequestAttributeIfNotPresent(request, RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());
    if (servletName != null) {
      exposeRequestAttributeIfNotPresent(request, RequestDispatcher.ERROR_SERVLET_NAME, servletName);
    }
  }

  /**
   * Expose the specified request attribute if not already present.
   *
   * @param request current servlet request
   * @param name the name of the attribute
   * @param value the suggested value of the attribute
   * @since 4.0
   */
  private static void exposeRequestAttributeIfNotPresent(ServletRequest request, String name, Object value) {
    if (request.getAttribute(name) == null) {
      request.setAttribute(name, value);
    }
  }

  /**
   * Clear the Servlet spec's error attributes as {@link HttpServletRequest}
   * attributes under the keys defined in the Servlet 2.3 specification:
   * {@code jakarta.servlet.error.status_code},
   * {@code jakarta.servlet.error.exception_type},
   * {@code jakarta.servlet.error.message},
   * {@code jakarta.servlet.error.exception},
   * {@code jakarta.servlet.error.request_uri},
   * {@code jakarta.servlet.error.servlet_name}.
   *
   * @param request current servlet request
   * @since 4.0
   */
  public static void clearErrorRequestAttributes(HttpServletRequest request) {
    request.removeAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    request.removeAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
    request.removeAttribute(RequestDispatcher.ERROR_MESSAGE);
    request.removeAttribute(RequestDispatcher.ERROR_EXCEPTION);
    request.removeAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    request.removeAttribute(RequestDispatcher.ERROR_SERVLET_NAME);
  }

  //---------------------------------------------------------------------
  // ServletContext
  //---------------------------------------------------------------------

  /**
   * Set a system property to the web application root directory.
   * The key of the system property can be defined with the "webAppRootKey"
   * context-param in {@code web.xml}. Default is "webapp.root".
   * <p>Can be used for tools that support substitution with {@code System.getProperty}
   * values, like log4j's "${key}" syntax within log file locations.
   *
   * @param servletContext the servlet context of the web application
   * @throws IllegalStateException if the system property is already set,
   * or if the WAR file is not expanded
   * @see #WEB_APP_ROOT_KEY_PARAM
   * @see #DEFAULT_WEB_APP_ROOT_KEY
   */
  public static void setWebAppRootSystemProperty(ServletContext servletContext) throws IllegalStateException {
    Assert.notNull(servletContext, "ServletContext must not be null");
    String root = servletContext.getRealPath("/");
    if (root == null) {
      throw new IllegalStateException(
              "Cannot set web app root system property when WAR file is not expanded");
    }
    String param = servletContext.getInitParameter(WEB_APP_ROOT_KEY_PARAM);
    String key = (param != null ? param : DEFAULT_WEB_APP_ROOT_KEY);
    String oldValue = System.getProperty(key);
    if (oldValue != null && !StringUtils.pathEquals(oldValue, root)) {
      throw new IllegalStateException(
              "Web app root system property already set to different value: '" +
                      key + "' = [" + oldValue + "] instead of [" + root + "] - " +
                      "Choose unique values for the 'webAppRootKey' context-param in your web.xml files!");
    }
    System.setProperty(key, root);
    servletContext.log("Set web app root system property: '" + key + "' = [" + root + "]");
  }

  /**
   * Remove the system property that points to the web app root directory.
   * To be called on shutdown of the web application.
   *
   * @param servletContext the servlet context of the web application
   * @see #setWebAppRootSystemProperty
   */
  public static void removeWebAppRootSystemProperty(ServletContext servletContext) {
    Assert.notNull(servletContext, "ServletContext must not be null");
    String param = servletContext.getInitParameter(WEB_APP_ROOT_KEY_PARAM);
    String key = (param != null ? param : DEFAULT_WEB_APP_ROOT_KEY);
    System.getProperties().remove(key);
  }

  /**
   * Return whether default HTML escaping is enabled for the web application,
   * i.e. the value of the "defaultHtmlEscape" context-param in {@code web.xml}
   * (if any).
   * <p>This method differentiates between no param specified at all and
   * an actual boolean value specified, allowing to have a context-specific
   * default in case of no setting at the global level.
   *
   * @param servletContext the servlet context of the web application
   * @return whether default HTML escaping is enabled for the given application
   * ({@code null} = no explicit default)
   */
  @Nullable
  public static Boolean getDefaultHtmlEscape(@Nullable ServletContext servletContext) {
    if (servletContext == null) {
      return null;
    }
    String param = servletContext.getInitParameter(HTML_ESCAPE_CONTEXT_PARAM);
    return StringUtils.hasText(param) ? Boolean.valueOf(param) : null;
  }

  /**
   * Return whether response encoding should be used when HTML escaping characters,
   * thus only escaping XML markup significant characters with UTF-* encodings.
   * This option is enabled for the web application with a ServletContext param,
   * i.e. the value of the "responseEncodedHtmlEscape" context-param in {@code web.xml}
   * (if any).
   * <p>This method differentiates between no param specified at all and
   * an actual boolean value specified, allowing to have a context-specific
   * default in case of no setting at the global level.
   *
   * @param servletContext the servlet context of the web application
   * @return whether response encoding is to be used for HTML escaping
   * ({@code null} = no explicit default)
   * @since 4.0
   */
  @Nullable
  public static Boolean getResponseEncodedHtmlEscape(@Nullable ServletContext servletContext) {
    if (servletContext == null) {
      return null;
    }
    String param = servletContext.getInitParameter(RESPONSE_ENCODED_HTML_ESCAPE_CONTEXT_PARAM);
    return StringUtils.hasText(param) ? Boolean.valueOf(param) : null;
  }

  /**
   * Return the temporary directory for the current web application,
   * as provided by the servlet container.
   *
   * @param servletContext the servlet context of the web application
   * @return the File representing the temporary directory
   */
  public static File getTempDir(ServletContext servletContext) {
    Assert.notNull(servletContext, "ServletContext must not be null");
    return (File) servletContext.getAttribute(TEMP_DIR_CONTEXT_ATTRIBUTE);
  }

  /**
   * Return the real path of the given path within the web application,
   * as provided by the servlet container.
   * <p>Prepends a slash if the path does not already start with a slash,
   * and throws a FileNotFoundException if the path cannot be resolved to
   * a resource (in contrast to ServletContext's {@code getRealPath},
   * which returns null).
   *
   * @param servletContext the servlet context of the web application
   * @param path the path within the web application
   * @return the corresponding real path
   * @throws FileNotFoundException if the path cannot be resolved to a resource
   * @see ServletContext#getRealPath
   */
  public static String getRealPath(ServletContext servletContext, String path) throws FileNotFoundException {
    Assert.notNull(servletContext, "ServletContext must not be null");
    // Interpret location as relative to the web application root directory.
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    String realPath = servletContext.getRealPath(path);
    if (realPath == null) {
      throw new FileNotFoundException(
              "ServletContext resource [" + path + "] cannot be resolved to absolute file path - " +
                      "web application archive not expanded?");
    }
    return realPath;
  }

}
