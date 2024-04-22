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

package cn.taketoday.framework.web.error;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.framework.web.netty.SendErrorHandler;
import cn.taketoday.framework.web.server.ConfigurableWebServerFactory;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.InvalidMediaTypeException;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.util.WebUtils;

/**
 * Basic global error {@link Controller @Controller}, rendering {@link ErrorAttributes}.
 * More specific errors can be handled either using Web MVC abstractions (e.g.
 * {@code @ExceptionHandler}) or by adding servlet
 * {@link ConfigurableWebServerFactory#setErrorPages server error pages}.
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
public class BasicErrorController extends AbstractErrorController implements SendErrorHandler {

  private final ReturnValueHandlerManager returnValueHandler;

  /**
   * Create a new {@link BasicErrorController} instance.
   *
   * @param errorAttributes the error attributes
   * @param errorProperties configuration properties
   * @param errorViewResolvers error view resolvers
   */
  public BasicErrorController(ErrorAttributes errorAttributes, ErrorProperties errorProperties,
          List<ErrorViewResolver> errorViewResolvers, ReturnValueHandlerManager returnValueHandler) {
    super(errorAttributes, errorProperties, errorViewResolvers);
    this.returnValueHandler = returnValueHandler;
  }

  @RequestMapping("${server.error.path:${error.path:/error}}")
  public Object error(RequestContext request) {
    return handleRequest(request, null);
  }

  @Override
  public void handleError(RequestContext request, @Nullable String message) {
    Object returnValue = handleRequest(request, message);
    if (returnValue != HttpRequestHandler.NONE_RETURN_VALUE) {
      try {
        returnValueHandler.handleReturnValue(request, null, returnValue);
      }
      catch (Throwable e) {
        throw ExceptionUtils.sneakyThrow(e);
      }
    }
  }

  private Object handleRequest(RequestContext request, @Nullable String message) {
    if (StringUtils.hasText(message)) {
      request.setAttribute(WebUtils.ERROR_MESSAGE_ATTRIBUTE, message);
    }

    HttpStatus status = getStatus(request);
    if (status.is2xxSuccessful()) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    boolean acceptsTextHtml = ifAcceptsTextHtml(request);
    Map<String, Object> model = getErrorAttributes(request, acceptsTextHtml ? MediaType.TEXT_HTML : MediaType.ALL);
    request.setStatus(status);
    if (acceptsTextHtml) {
      request.setContentType(MediaType.TEXT_HTML);
      return resolveErrorView(request, status, model);
    }
    return model;
  }

  /**
   * Predicate that checks whether the current request explicitly support
   * {@code "text/html"} media type.
   * <p>
   * The "match-all" media type is not considered here.
   */
  static boolean ifAcceptsTextHtml(RequestContext context) {
    try {
      ArrayList<MediaType> acceptedMediaTypes = new ArrayList<>(context.getHeaders().getAccept());
      acceptedMediaTypes.removeIf(MediaType.ALL::equalsTypeAndSubtype);
      MimeTypeUtils.sortBySpecificity(acceptedMediaTypes);
      return acceptedMediaTypes.stream().anyMatch(MediaType.TEXT_HTML::isCompatibleWith);
    }
    catch (InvalidMediaTypeException ex) {
      return false;
    }
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<String> mediaTypeNotAcceptable(RequestContext request) {
    HttpStatus status = getStatus(request);
    return ResponseEntity.status(status).build();
  }

}
