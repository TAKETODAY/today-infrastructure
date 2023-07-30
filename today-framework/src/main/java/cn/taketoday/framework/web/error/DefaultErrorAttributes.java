/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.web.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.core.Ordered;
import cn.taketoday.framework.web.error.ErrorAttributeOptions.Include;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCodeProvider;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.ObjectError;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.util.WebUtils;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;

/**
 * Default implementation of {@link ErrorAttributes}. Provides the following attributes
 * when possible:
 * <ul>
 * <li>timestamp - The time that the errors were extracted</li>
 * <li>status - The status code</li>
 * <li>error - The error reason</li>
 * <li>exception - The class name of the root exception (if configured)</li>
 * <li>message - The exception message (if configured)</li>
 * <li>errors - Any {@link ObjectError}s from a {@link BindingResult} exception (if
 * configured)</li>
 * <li>trace - The exception stack trace (if configured)</li>
 * <li>path - The URL path when the exception was raised</li>
 * <li>requestId - Unique ID associated with the current request</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Scott Frederick
 * @see ErrorAttributes
 * @since 4.0
 */
public class DefaultErrorAttributes implements ErrorAttributes, Ordered {

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public Map<String, Object> getErrorAttributes(RequestContext context, ErrorAttributeOptions options) {
    HashMap<String, Object> errorAttributes = new HashMap<>();
    errorAttributes.put("timestamp", new Date());
    addPath(context, errorAttributes);
    addStatus(errorAttributes, context);
    addErrorDetails(errorAttributes, context, options);
    errorAttributes.put("requestId", context.getRequestId());
    return errorAttributes;
  }

  private void addPath(RequestContext request, HashMap<String, Object> attributes) {
    if (ServletDetector.runningInServlet(request)) {
      Object path = getAttribute(request, RequestDispatcher.ERROR_REQUEST_URI);
      if (path == null) {
        path = request.getRequestURI();
      }
      attributes.put("path", path);
    }
    else {
      attributes.put("path", request.getRequestURI());
    }
  }

  private void addStatus(Map<String, Object> attributes, RequestContext request) {
    Integer status;
    if (ServletDetector.runningInServlet(request)) {
      status = getAttribute(request, RequestDispatcher.ERROR_STATUS_CODE);
    }
    else {
      status = request.getStatus();
    }

    Throwable error = getError(request);
    if (error instanceof HttpStatusCodeProvider provider) {
      status = provider.getStatusCode().value();
    }

    if (status == null) {
      attributes.put("status", 999);
      attributes.put("error", "None");
      return;
    }

    attributes.put("status", status);
    try {
      attributes.put("error", HttpStatus.valueOf(status).getReasonPhrase());
    }
    catch (Exception ex) {
      // Unable to obtain a reason
      attributes.put("error", "Http Status " + status);
    }
  }

  private void addErrorDetails(Map<String, Object> attributes, RequestContext request, ErrorAttributeOptions options) {
    Throwable error = getError(request);
    if (error != null) {
      if (ServletDetector.runningInServlet(request)) {
        while (error instanceof ServletException && error.getCause() != null) {
          error = error.getCause();
        }
      }

      if (options.isIncluded(Include.EXCEPTION)) {
        attributes.put("exception", error.getClass().getName());
      }

      if (options.isIncluded(Include.STACK_TRACE)) {
        StringWriter stackTrace = new StringWriter();
        error.printStackTrace(new PrintWriter(stackTrace));
        stackTrace.flush();
        attributes.put("trace", stackTrace.toString());
      }
    }

    if (error instanceof BindingResult result) {
      if (options.isIncluded(Include.BINDING_ERRORS)) {
        attributes.put("errors", result.getAllErrors());
      }
      if (options.isIncluded(Include.MESSAGE)) {
        attributes.put("message", "Validation failed for object='" +
                result.getObjectName() + "'. " + "Error count: " + result.getErrorCount());
      }
    }
    else if (options.isIncluded(Include.MESSAGE)) {
      attributes.put("message", getMessage(request, error));
    }
  }

  /**
   * Returns the message to be included as the value of the {@code message} error
   * attribute. By default the returned message is the first of the following that is
   * not empty:
   * <ol>
   * <li>Value of the {@link RequestDispatcher#ERROR_MESSAGE} request attribute.
   * <li>Message of the given {@code error}.
   * <li>{@code No message available}.
   * </ol>
   *
   * @param request current request
   * @param error current error, if any
   * @return message to include in the error attributes
   */
  protected String getMessage(RequestContext request, Throwable error) {
    if (ServletDetector.runningInServlet(request)) {
      Object attribute = getAttribute(request, RequestDispatcher.ERROR_MESSAGE);
      if (attribute instanceof String message) {
        return message;
      }
    }
    if (error != null && StringUtils.hasText(error.getMessage())) {
      return error.getMessage();
    }
    return "No message available";
  }

  @Override
  @Nullable
  public Throwable getError(RequestContext request) {
    Object attribute = getAttribute(request, WebUtils.ERROR_EXCEPTION_ATTRIBUTE);
    if (!(attribute instanceof Throwable)) {
      if (ServletDetector.runningInServlet(request)) {
        attribute = getAttribute(request, RequestDispatcher.ERROR_EXCEPTION);
      }
    }

    if (attribute instanceof Throwable exception) {
      // store the exception in a well-known attribute to make it available to metrics
      // instrumentation.
      request.setAttribute(ErrorAttributes.ERROR_ATTRIBUTE, attribute);
      return exception;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private <T> T getAttribute(RequestContext requestAttributes, String name) {
    return (T) requestAttributes.getAttribute(name);
  }

}
