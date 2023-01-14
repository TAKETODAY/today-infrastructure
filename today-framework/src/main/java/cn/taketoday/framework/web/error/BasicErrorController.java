/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.web.error;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.framework.web.error.ErrorAttributeOptions.Include;
import cn.taketoday.framework.web.servlet.server.AbstractServletWebServerFactory;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Assert;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.view.ModelAndView;

/**
 * Basic global error {@link Controller @Controller}, rendering {@link ErrorAttributes}.
 * More specific errors can be handled either using Spring MVC abstractions (e.g.
 * {@code @ExceptionHandler}) or by adding servlet
 * {@link AbstractServletWebServerFactory#setErrorPages server error pages}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Michael Stummvoll
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ErrorAttributes
 * @see ErrorProperties
 * @since 4.0
 */
@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class BasicErrorController extends AbstractErrorController {

  private final ErrorProperties errorProperties;

  /**
   * Create a new {@link BasicErrorController} instance.
   *
   * @param errorAttributes the error attributes
   * @param errorProperties configuration properties
   */
  public BasicErrorController(ErrorAttributes errorAttributes, ErrorProperties errorProperties) {
    this(errorAttributes, errorProperties, Collections.emptyList());
  }

  /**
   * Create a new {@link BasicErrorController} instance.
   *
   * @param errorAttributes the error attributes
   * @param errorProperties configuration properties
   * @param errorViewResolvers error view resolvers
   */
  public BasicErrorController(ErrorAttributes errorAttributes,
          ErrorProperties errorProperties, List<ErrorViewResolver> errorViewResolvers) {
    super(errorAttributes, errorViewResolvers);
    Assert.notNull(errorProperties, "ErrorProperties must not be null");
    this.errorProperties = errorProperties;
  }

  @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
  public Object errorHtml(RequestContext request) {
    HttpStatus status = getStatus(request);
    Map<String, Object> model = Collections.unmodifiableMap(
            getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.TEXT_HTML)));
    request.setStatus(status.value());
    Object view = resolveErrorView(request, status, model);
    return view != null ? view : new ModelAndView("error", model);
  }

  @RequestMapping
  public ResponseEntity<Map<String, Object>> error(RequestContext request) {
    HttpStatus status = getStatus(request);
    if (status == HttpStatus.NO_CONTENT) {
      return new ResponseEntity<>(status);
    }
    Map<String, Object> body = getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.ALL));
    return new ResponseEntity<>(body, status);
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<String> mediaTypeNotAcceptable(RequestContext request) {
    HttpStatus status = getStatus(request);
    return ResponseEntity.status(status).build();
  }

  protected ErrorAttributeOptions getErrorAttributeOptions(RequestContext request, MediaType mediaType) {
    ErrorAttributeOptions options = ErrorAttributeOptions.defaults();
    if (this.errorProperties.isIncludeException()) {
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
    return switch (getErrorProperties().getIncludeStacktrace()) {
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
    return switch (getErrorProperties().getIncludeMessage()) {
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
    return switch (getErrorProperties().getIncludeBindingErrors()) {
      case ALWAYS -> true;
      case ON_PARAM -> getErrorsParameter(request);
      default -> false;
    };
  }

  /**
   * Provide access to the error properties.
   *
   * @return the error properties
   */
  protected ErrorProperties getErrorProperties() {
    return this.errorProperties;
  }

}
