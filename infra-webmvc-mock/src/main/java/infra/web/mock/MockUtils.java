/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.mock;

import org.jspecify.annotations.Nullable;

import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.util.ObjectUtils;
import infra.web.RequestContext;
import infra.web.mock.api.MockIndicator;
import infra.web.util.WebUtils;

/**
 * @author TODAY 2020/12/8 23:07
 * @since 3.0
 */
@SuppressWarnings("NullAway")
public abstract class MockUtils {

  /** Name suffixes in case of image buttons.  @since 4.0 */
  public static final String[] SUBMIT_IMAGE_SUFFIXES = { ".x", ".y" };

  /**
   * Return an appropriate MockRequest object
   *
   * @param context the context to introspect
   * @return the matching request object
   * @see WebUtils#getNativeContext(RequestContext, Class)
   */
  public static MockRequest getMockRequest(RequestContext context) {
    if (context instanceof MockIndicator mockIndicator) {
      return mockIndicator.getRequest();
    }
    MockIndicator nativeContext = WebUtils.getNativeContext(context, MockIndicator.class);
    Assert.state(nativeContext != null, "Not run in mock");
    return nativeContext.getRequest();
  }

  /**
   * Return an appropriate response object
   *
   * @param context the context to introspect
   * @return the matching response object
   * @see WebUtils#getNativeContext(RequestContext, Class)
   */
  public static MockResponse getMockResponse(RequestContext context) {
    if (context instanceof MockIndicator mockIndicator) {
      return mockIndicator.getResponse();
    }
    MockIndicator nativeContext = WebUtils.getNativeContext(context, MockIndicator.class);
    Assert.state(nativeContext != null, "Not run in mock");
    return nativeContext.getResponse();
  }

  //---------------------------------------------------------------------
  // MockRequest
  //---------------------------------------------------------------------

  /**
   * @since 4.0
   */
  public static boolean isPostForm(MockRequest request) {
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

}
