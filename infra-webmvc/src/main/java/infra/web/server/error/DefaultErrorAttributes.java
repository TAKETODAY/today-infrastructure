/*
 * Copyright 2012-present the original author or authors.
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

package infra.web.server.error;

import org.jspecify.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import infra.core.Ordered;
import infra.http.HttpStatus;
import infra.util.StringUtils;
import infra.validation.BindingResult;
import infra.validation.ObjectError;
import infra.web.HttpStatusProvider;
import infra.web.RequestContext;
import infra.web.server.ResponseStatusException;
import infra.web.util.WebUtils;
import infra.web.server.error.ErrorAttributeOptions.Include;

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
    errorAttributes.put("timestamp", Instant.now());
    addPath(context, errorAttributes);
    addStatus(errorAttributes, context);
    addErrorDetails(errorAttributes, context, options);

    if (!options.isIncluded(Include.PATH)) {
      errorAttributes.remove("path");
    }

    return errorAttributes;
  }

  private void addPath(RequestContext request, HashMap<String, Object> attributes) {
    attributes.put("path", request.getRequestURI());
  }

  private void addStatus(Map<String, Object> attributes, RequestContext request) {
    int status = request.getStatus();

    Throwable error = getError(request);
    if (error instanceof HttpStatusProvider provider) {
      status = provider.getStatusCode().value();
    }

    if (status == 200) {
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

  @SuppressWarnings("NullAway")
  private void addErrorDetails(Map<String, Object> attributes, RequestContext request, ErrorAttributeOptions options) {
    Throwable error = getError(request);
    if (error != null) {
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
        attributes.put("message", "Validation failed for object='%s'. Error count: %d"
                .formatted(result.getObjectName(), result.getErrorCount()));
      }
    }
    else if (error instanceof ResponseStatusException rse) {
      if (options.isIncluded(Include.MESSAGE)) {
        attributes.put("message", rse.getReason());
      }
      error = rse.getCause() != null ? rse.getCause() : error;
      if (error instanceof BindingResult bindingResult) {
        if (options.isIncluded(Include.BINDING_ERRORS)) {
          attributes.put("errors", bindingResult.getAllErrors());
        }
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
   * <li>Value of the {@link WebUtils#ERROR_MESSAGE_ATTRIBUTE} request attribute.
   * <li>Message of the given {@code error}.
   * <li>{@code No message available}.
   * </ol>
   *
   * @param request current request
   * @param error current error, if any
   * @return message to include in the error attributes
   */
  protected String getMessage(RequestContext request, @Nullable Throwable error) {
    Object attribute = request.getAttribute(WebUtils.ERROR_MESSAGE_ATTRIBUTE);
    if (attribute instanceof String message && StringUtils.hasText(message)) {
      return message;
    }

    if (error != null && StringUtils.hasText(error.getMessage())) {
      return error.getMessage();
    }
    return "No message available";
  }

  @Override
  @Nullable
  public Throwable getError(RequestContext request) {
    Object attribute = request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE);
    if (attribute instanceof Throwable exception) {
      return exception;
    }
    return null;
  }

}
