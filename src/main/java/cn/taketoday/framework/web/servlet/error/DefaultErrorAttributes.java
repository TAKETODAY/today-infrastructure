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

package cn.taketoday.framework.web.servlet.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.core.Order;
import cn.taketoday.core.Ordered;
import cn.taketoday.framework.web.error.ErrorAttributeOptions;
import cn.taketoday.framework.web.error.ErrorAttributeOptions.Include;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.ObjectError;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerExceptionHandler;
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
 * </ul>
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Scott Frederick
 * @see ErrorAttributes
 * @since 4.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DefaultErrorAttributes implements ErrorAttributes, HandlerExceptionHandler, Ordered {

  private static final String ERROR_INTERNAL_ATTRIBUTE = DefaultErrorAttributes.class.getName() + ".ERROR";

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Nullable
  @Override
  public Object handleException(RequestContext context, Throwable ex, @Nullable Object handler) {
    storeErrorAttributes(context, ex);
    return NONE_RETURN_VALUE;
  }

  private void storeErrorAttributes(RequestContext request, Throwable ex) {
    request.setAttribute(ERROR_INTERNAL_ATTRIBUTE, ex);
  }

  @Override
  public Map<String, Object> getErrorAttributes(RequestContext webRequest, ErrorAttributeOptions options) {
    Map<String, Object> errorAttributes = getErrorAttributes(webRequest, options.isIncluded(Include.STACK_TRACE));
    if (!options.isIncluded(Include.EXCEPTION)) {
      errorAttributes.remove("exception");
    }
    if (!options.isIncluded(Include.STACK_TRACE)) {
      errorAttributes.remove("trace");
    }
    if (!options.isIncluded(Include.MESSAGE) && errorAttributes.get("message") != null) {
      errorAttributes.remove("message");
    }
    if (!options.isIncluded(Include.BINDING_ERRORS)) {
      errorAttributes.remove("errors");
    }
    return errorAttributes;
  }

  private Map<String, Object> getErrorAttributes(RequestContext context, boolean includeStackTrace) {
    Map<String, Object> errorAttributes = new LinkedHashMap<>();
    errorAttributes.put("timestamp", new Date());
    addStatus(errorAttributes, context);
    addErrorDetails(errorAttributes, context, includeStackTrace);
    addPath(errorAttributes, context);
    return errorAttributes;
  }

  private void addStatus(Map<String, Object> errorAttributes, RequestContext requestAttributes) {
    Integer status = getAttribute(requestAttributes, RequestDispatcher.ERROR_STATUS_CODE);
    if (status == null) {
      errorAttributes.put("status", 999);
      errorAttributes.put("error", "None");
      return;
    }
    errorAttributes.put("status", status);
    try {
      errorAttributes.put("error", HttpStatus.valueOf(status).getReasonPhrase());
    }
    catch (Exception ex) {
      // Unable to obtain a reason
      errorAttributes.put("error", "Http Status " + status);
    }
  }

  private void addErrorDetails(Map<String, Object> errorAttributes, RequestContext webRequest,
                               boolean includeStackTrace) {
    Throwable error = getError(webRequest);
    if (error != null) {
      while (error instanceof ServletException && error.getCause() != null) {
        error = error.getCause();
      }
      errorAttributes.put("exception", error.getClass().getName());
      if (includeStackTrace) {
        addStackTrace(errorAttributes, error);
      }
    }
    addErrorMessage(errorAttributes, webRequest, error);
  }

  private void addErrorMessage(Map<String, Object> errorAttributes, RequestContext webRequest, Throwable error) {
    BindingResult result = extractBindingResult(error);
    if (result == null) {
      addExceptionErrorMessage(errorAttributes, webRequest, error);
    }
    else {
      addBindingResultErrorMessage(errorAttributes, result);
    }
  }

  private void addExceptionErrorMessage(
          Map<String, Object> errorAttributes, RequestContext webRequest, Throwable error) {
    errorAttributes.put("message", getMessage(webRequest, error));
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
   * @param webRequest current request
   * @param error current error, if any
   * @return message to include in the error attributes
   */
  protected String getMessage(RequestContext webRequest, Throwable error) {
    Object message = getAttribute(webRequest, RequestDispatcher.ERROR_MESSAGE);
    if (!ObjectUtils.isEmpty(message)) {
      return message.toString();
    }
    if (error != null && StringUtils.isNotEmpty(error.getMessage())) {
      return error.getMessage();
    }
    return "No message available";
  }

  private void addBindingResultErrorMessage(Map<String, Object> errorAttributes, BindingResult result) {
    errorAttributes.put("message", "Validation failed for object='" + result.getObjectName() + "'. "
            + "Error count: " + result.getErrorCount());
    errorAttributes.put("errors", result.getAllErrors());
  }

  private BindingResult extractBindingResult(Throwable error) {
    if (error instanceof BindingResult) {
      return (BindingResult) error;
    }
    return null;
  }

  private void addStackTrace(Map<String, Object> errorAttributes, Throwable error) {
    StringWriter stackTrace = new StringWriter();
    error.printStackTrace(new PrintWriter(stackTrace));
    stackTrace.flush();
    errorAttributes.put("trace", stackTrace.toString());
  }

  private void addPath(Map<String, Object> errorAttributes, RequestContext requestAttributes) {
    String path = getAttribute(requestAttributes, RequestDispatcher.ERROR_REQUEST_URI);
    if (path != null) {
      errorAttributes.put("path", path);
    }
  }

  @Override
  @Nullable
  public Throwable getError(RequestContext webRequest) {
    Throwable exception = getAttribute(webRequest, ERROR_INTERNAL_ATTRIBUTE);
    if (exception == null) {
      exception = getAttribute(webRequest, RequestDispatcher.ERROR_EXCEPTION);
    }
    // store the exception in a well-known attribute to make it available to metrics
    // instrumentation.
    webRequest.setAttribute(ErrorAttributes.ERROR_ATTRIBUTE, exception);
    return exception;
  }

  @SuppressWarnings("unchecked")
  private <T> T getAttribute(RequestContext requestAttributes, String name) {
    return (T) requestAttributes.getAttribute(name);
  }

}
