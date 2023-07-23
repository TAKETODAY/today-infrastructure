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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.view.ModelAndView;

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

  private final ErrorAttributes errorAttributes;

  private final List<ErrorViewResolver> errorViewResolvers;

  public AbstractErrorController(ErrorAttributes errorAttributes) {
    this(errorAttributes, null);
  }

  public AbstractErrorController(ErrorAttributes errorAttributes,
          @Nullable List<ErrorViewResolver> errorViewResolvers) {
    Assert.notNull(errorAttributes, "ErrorAttributes is required");
    this.errorAttributes = errorAttributes;
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

  protected Map<String, Object> getErrorAttributes(RequestContext request, ErrorAttributeOptions options) {
    return this.errorAttributes.getErrorAttributes(request, options);
  }

  protected boolean getTraceParameter(RequestContext request) {
    return getBooleanParameter(request, "trace");
  }

  protected boolean getMessageParameter(RequestContext request) {
    return getBooleanParameter(request, "message");
  }

  protected boolean getErrorsParameter(RequestContext request) {
    return getBooleanParameter(request, "errors");
  }

  protected boolean getBooleanParameter(RequestContext request, String parameterName) {
    String parameter = request.getParameter(parameterName);
    if (parameter == null) {
      return false;
    }
    return !"false".equalsIgnoreCase(parameter);
  }

  protected HttpStatus getStatus(RequestContext request) {
    // TODO statusCode 获取问题
    int statusCode = request.getStatus();
    try {
      return HttpStatus.valueOf(statusCode);
    }
    catch (Exception ex) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
  }

  /**
   * Resolve any specific error views. By default this method delegates to
   * {@link ErrorViewResolver ErrorViewResolvers}.
   *
   * @param request the request
   * @param status the HTTP status
   * @param model the suggested model
   * @return a specific {@link ModelAndView} or {@code null} if the default should be
   * used
   */
  @Nullable
  protected Object resolveErrorView(RequestContext request,
          HttpStatus status, Map<String, Object> model) {
    for (ErrorViewResolver resolver : errorViewResolvers) {
      Object view = resolver.resolveErrorView(request, status, model);
      if (view != null) {
        return view;
      }
    }
    return null;
  }

}
