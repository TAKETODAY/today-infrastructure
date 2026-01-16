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

package infra.webmvc.error;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import infra.app.config.web.ErrorProperties;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.stereotype.Controller;
import infra.web.RequestContext;
import infra.web.view.ModelAndView;
import infra.webmvc.error.ErrorAttributeOptions.Include;

/**
 * Abstract base class for error {@link Controller @Controller} implementations.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ErrorAttributes
 * @since 4.0 2023/1/14 15:49
 */
public abstract class AbstractErrorController implements ErrorController {

  protected final ErrorAttributes errorAttributes;

  protected final ErrorProperties errorProperties;

  protected final List<ErrorViewResolver> errorViewResolvers;

  public AbstractErrorController(ErrorAttributes errorAttributes,
          ErrorProperties errorProperties, @Nullable List<ErrorViewResolver> errorViewResolvers) {
    Assert.notNull(errorAttributes, "ErrorAttributes is required");
    Assert.notNull(errorProperties, "ErrorProperties is required");
    this.errorAttributes = errorAttributes;
    this.errorProperties = errorProperties;
    this.errorViewResolvers = sortErrorViewResolvers(errorViewResolvers);
  }

  private List<ErrorViewResolver> sortErrorViewResolvers(@Nullable List<ErrorViewResolver> resolvers) {
    ArrayList<ErrorViewResolver> sorted = new ArrayList<>();
    if (resolvers != null) {
      sorted.addAll(resolvers);
      AnnotationAwareOrderComparator.sort(sorted);
    }
    return sorted;
  }

  protected Map<String, Object> getErrorAttributes(RequestContext request, MediaType mediaType) {
    return this.errorAttributes.getErrorAttributes(request, getErrorAttributeOptions(request, mediaType));
  }

  /**
   * Returns whether the trace parameter is set.
   *
   * @param request the request
   * @return whether the trace parameter is set
   */
  protected boolean getTraceParameter(RequestContext request) {
    return getBooleanParameter(request, "trace");
  }

  /**
   * Returns whether the message parameter is set.
   *
   * @param request the request
   * @return whether the message parameter is set
   */
  protected boolean getMessageParameter(RequestContext request) {
    return getBooleanParameter(request, "message");
  }

  /**
   * Returns whether the errors parameter is set.
   *
   * @param request the request
   * @return whether the errors parameter is set
   */
  protected boolean getErrorsParameter(RequestContext request) {
    return getBooleanParameter(request, "errors");
  }

  /**
   * Returns whether the path parameter is set.
   *
   * @param request the request
   * @return whether the path parameter is set
   */
  protected boolean getPathParameter(RequestContext request) {
    return getBooleanParameter(request, "path");
  }

  protected boolean getBooleanParameter(RequestContext request, String parameterName) {
    String parameter = request.getParameter(parameterName);
    if (parameter == null) {
      return false;
    }
    return !"false".equalsIgnoreCase(parameter);
  }

  protected HttpStatus getStatus(RequestContext request) {
    int statusCode = request.getStatus();
    try {
      return HttpStatus.valueOf(statusCode);
    }
    catch (Exception ex) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
  }

  /**
   * Resolve any specific error views. By default, this method delegates to
   * {@link ErrorViewResolver ErrorViewResolvers}.
   *
   * @param request the request
   * @param status the HTTP status
   * @param model the suggested model
   * @return a specific {@link ModelAndView} or {@code null} if the default should be
   * used
   */
  protected ModelAndView resolveErrorView(RequestContext request, HttpStatusCode status, Map<String, Object> model) {
    for (ErrorViewResolver resolver : errorViewResolvers) {
      ModelAndView view = resolver.resolveErrorView(request, status, model);
      if (view != null) {
        return view;
      }
    }
    return new ModelAndView("error", model);
  }

  protected ErrorAttributeOptions getErrorAttributeOptions(RequestContext request, MediaType mediaType) {
    ErrorAttributeOptions options = ErrorAttributeOptions.defaults();
    if (errorProperties.includeException) {
      options = options.including(Include.EXCEPTION);
    }
    if (isIncludeStackTrace(request, mediaType)) {
      options = options.including(Include.STACK_TRACE);
    }
    if (isIncludeMessage(request, mediaType)) {
      options = options.including(Include.MESSAGE);
    }
    if (isIncludeBindingErrors(request, mediaType)) {
      options = options.including(Include.BINDING_ERRORS);
    }

    options = isIncludePath(request, mediaType) ? options.including(Include.PATH) : options.excluding(Include.PATH);

    return options;
  }

  /**
   * Determine if the stacktrace attribute should be included.
   *
   * @param request the source request
   * @param produces the media type produced (or {@code MediaType.ALL})
   * @return if the stacktrace attribute should be included
   */
  protected boolean isIncludeStackTrace(RequestContext request, MediaType produces) {
    return switch (errorProperties.includeStacktrace) {
      case ALWAYS -> true;
      case ON_PARAM -> getTraceParameter(request);
      default -> false;
    };
  }

  /**
   * Determine if the message attribute should be included.
   *
   * @param request the source request
   * @param produces the media type produced (or {@code MediaType.ALL})
   * @return if the message attribute should be included
   */
  protected boolean isIncludeMessage(RequestContext request, MediaType produces) {
    return switch (errorProperties.includeMessage) {
      case ALWAYS -> true;
      case ON_PARAM -> getMessageParameter(request);
      default -> false;
    };
  }

  /**
   * Determine if the errors attribute should be included.
   *
   * @param request the source request
   * @param produces the media type produced (or {@code MediaType.ALL})
   * @return if the errors attribute should be included
   */
  protected boolean isIncludeBindingErrors(RequestContext request, MediaType produces) {
    return switch (errorProperties.includeBindingErrors) {
      case ALWAYS -> true;
      case ON_PARAM -> getErrorsParameter(request);
      default -> false;
    };
  }

  /**
   * Determine if the path attribute should be included.
   *
   * @param request the source request
   * @param produces the media type produced (or {@code MediaType.ALL})
   * @return if the path attribute should be included
   */
  protected boolean isIncludePath(RequestContext request, MediaType produces) {
    return switch (errorProperties.includePath) {
      case ALWAYS -> true;
      case ON_PARAM -> getPathParameter(request);
      case NEVER -> false;
    };
  }

}
