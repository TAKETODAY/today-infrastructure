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

package infra.web.mock;

import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import infra.core.Conventions;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.mock.api.MockContext;
import infra.mock.api.MockRequest;
import infra.mock.api.MockRequestWrapper;
import infra.mock.api.MockResponse;
import infra.mock.api.MockResponseWrapper;
import infra.mock.api.RequestDispatcher;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.api.http.HttpMockResponse;
import infra.mock.api.http.HttpSession;
import infra.util.ObjectUtils;
import infra.web.MockIndicator;
import infra.web.RequestContext;
import infra.web.mock.support.WebApplicationContextUtils;
import infra.web.util.WebUtils;

/**
 * @author TODAY 2020/12/8 23:07
 * @since 3.0
 */
public abstract class MockUtils {

  /**
   * Request attribute to hold the current web application context.
   * Otherwise only the global web app context is obtainable by tags etc.
   */
  public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          MockUtils.class, "CONTEXT");

  /** Name suffixes in case of image buttons.  @since 4.0 */
  public static final String[] SUBMIT_IMAGE_SUFFIXES = { ".x", ".y" };

  /**
   * Standard Mock spec context attribute that specifies a temporary
   * directory for the current web application, of type {@code java.io.File}.
   *
   * @since 4.0
   */
  public static final String TEMP_DIR_CONTEXT_ATTRIBUTE = "infra.mock.api.context.tempdir";

  // context

  public static RequestContext getRequestContext(MockRequest request, MockResponse response) {
    HttpMockRequest servletRequest = (HttpMockRequest) request;
    return getRequestContext(findWebApplicationContext(servletRequest), servletRequest, (HttpMockResponse) response);
  }

  public static RequestContext getRequestContext(
          WebApplicationContext webApplicationContext, HttpMockRequest request, HttpMockResponse response) {
    return new MockRequestContext(webApplicationContext, request, response);
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
  public static HttpSession getHttpSession(RequestContext context, boolean create) {
    HttpMockRequest request = getMockRequest(context);
    return request.getSession(create);
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
  public static <T> T getNativeRequest(MockRequest request, @Nullable Class<T> requiredType) {
    if (requiredType != null) {
      if (requiredType.isInstance(request)) {
        return (T) request;
      }
      else if (request instanceof MockRequestWrapper wrapper) {
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
  public static <T> T getNativeResponse(MockResponse response, @Nullable Class<T> requiredType) {
    if (requiredType != null) {
      if (requiredType.isInstance(response)) {
        return (T) response;
      }
      else if (response instanceof MockResponseWrapper wrapper) {
        return getNativeResponse(wrapper.getResponse(), requiredType);
      }
    }
    return null;
  }

  /**
   * Return an appropriate HttpMockRequest object
   *
   * @param context the context to introspect
   * @return the matching request object
   * @see WebUtils#getNativeContext(RequestContext, Class)
   */
  public static HttpMockRequest getMockRequest(RequestContext context) {
    if (context instanceof MockIndicator mockIndicator) {
      return mockIndicator.getRequest();
    }
    MockIndicator nativeContext = WebUtils.getNativeContext(context, MockIndicator.class);
    Assert.state(nativeContext != null, "Not run in servlet");
    return nativeContext.getRequest();
  }

  /**
   * Gets the servlet context to which this MockRequest was last dispatched.
   *
   * @return the servlet context to which this MockRequest was last dispatched
   * @since 4.0
   */
  public static MockContext getMockContext(RequestContext context) {
    return getMockRequest(context).getMockContext();
  }

  /**
   * Return an appropriate response object
   *
   * @param context the context to introspect
   * @return the matching response object
   * @see WebUtils#getNativeContext(RequestContext, Class)
   */
  public static HttpMockResponse getMockResponse(RequestContext context) {
    if (context instanceof MockIndicator mockIndicator) {
      return mockIndicator.getResponse();
    }
    MockIndicator nativeContext = WebUtils.getNativeContext(context, MockIndicator.class);
    Assert.state(nativeContext != null, "Not run in servlet");
    return nativeContext.getResponse();
  }

  /**
   * Look for the WebApplicationContext associated with the DispatcherMock
   * that has initiated request processing, and for the global context if none
   * was found associated with the current request. The global context will
   * be found via the MockContext or via ContextLoader's current context.
   *
   * @param request current HTTP request
   * @return the request-specific WebApplicationContext, or the global one
   * if no request-specific context has been found, or {@code null} if none
   * @see #findWebApplicationContext(MockRequest, MockContext)
   * @see MockRequest#getMockContext()
   * @since 4.0
   */
  @Nullable
  public static WebApplicationContext findWebApplicationContext(MockRequest request) {
    return findWebApplicationContext(request, request.getMockContext());
  }

  /**
   * @param request current HTTP request
   * @param mockContext current servlet context
   * @return the request-specific WebApplicationContext, or the global one
   * if no request-specific context has been found, or {@code null} if none
   * @see #WEB_APPLICATION_CONTEXT_ATTRIBUTE
   * @see WebApplicationContextUtils#getWebApplicationContext(MockContext)
   * @since 4.0
   */
  @Nullable
  public static WebApplicationContext findWebApplicationContext(
          MockRequest request, @Nullable MockContext mockContext) {
    WebApplicationContext webApplicationContext = (WebApplicationContext) request.getAttribute(
            WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    if (webApplicationContext == null) {
      if (mockContext != null) {
        webApplicationContext = WebApplicationContextUtils.findWebApplicationContext(mockContext);
      }
    }
    return webApplicationContext;
  }

  //---------------------------------------------------------------------
  // MockRequest
  //---------------------------------------------------------------------

  /**
   * Determine whether the given request is an include request,
   * that is, not a top-level HTTP request coming in from the outside.
   * <p>Checks the presence of the "infra.mock.api.include.request_uri"
   * request attribute. Could check any request attribute that is only
   * present in an include request.
   *
   * @param request current servlet request
   * @return whether the given request is an include request
   * @since 4.0
   */
  public static boolean isIncludeRequest(MockRequest request) {
    return request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null;
  }

  /**
   * @since 4.0
   */
  public static boolean isPostForm(HttpMockRequest request) {
    String contentType = request.getContentType();
    return contentType != null
            && HttpMethod.POST.matches(request.getMethod())
            && contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
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
   * @see MockRequest#getParameterNames
   * @see MockRequest#getParameterValues
   * @see MockRequest#getParameterMap
   */
  public static Map<String, Object> getParametersStartingWith(MockRequest request, @Nullable String prefix) {
    Assert.notNull(request, "Request is required");
    Enumeration<String> paramNames = request.getParameterNames();
    Map<String, Object> params = new TreeMap<>();
    while (paramNames != null && paramNames.hasMoreElements()) {
      String paramName = paramNames.nextElement();
      if (prefix == null) {
        String[] values = request.getParameterValues(paramName);
        if (ObjectUtils.isNotEmpty(values)) {
          if (values.length > 1) {
            params.put(paramName, values);
          }
          else {
            params.put(paramName, values[0]);
          }
        }
      }
      else if (paramName.startsWith(prefix)) {
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

  //---------------------------------------------------------------------
  // MockContext
  //---------------------------------------------------------------------

  /**
   * Return the real path of the given path within the web application,
   * as provided by the servlet container.
   * <p>Prepends a slash if the path does not already start with a slash,
   * and throws a FileNotFoundException if the path cannot be resolved to
   * a resource (in contrast to MockContext's {@code getRealPath},
   * which returns null).
   *
   * @param mockContext the servlet context of the web application
   * @param path the path within the web application
   * @return the corresponding real path
   * @throws FileNotFoundException if the path cannot be resolved to a resource
   * @see MockContext#getRealPath
   */
  public static String getRealPath(MockContext mockContext, String path) throws FileNotFoundException {
    Assert.notNull(mockContext, "MockContext is required");
    // Interpret location as relative to the web application root directory.
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    String realPath = mockContext.getRealPath(path);
    if (realPath == null) {
      throw new FileNotFoundException(
              "MockContext resource [" + path + "] cannot be resolved to absolute file path - " +
                      "web application archive not expanded?");
    }
    return realPath;
  }

}
