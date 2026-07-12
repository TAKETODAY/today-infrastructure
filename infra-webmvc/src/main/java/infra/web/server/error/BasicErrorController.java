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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import infra.app.config.web.ErrorProperties;
import infra.http.HttpStatus;
import infra.http.InvalidMediaTypeException;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.stereotype.Controller;
import infra.util.ExceptionUtils;
import infra.util.MimeTypeUtils;
import infra.util.StringUtils;
import infra.web.HttpContext;
import infra.web.HttpMediaTypeNotAcceptableException;
import infra.web.HttpRequestHandler;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.RequestMapping;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.util.WebUtils;

/**
 * Basic global error {@link Controller @Controller}, rendering {@link ErrorAttributes}.
 * More specific errors can be handled either using Web MVC abstractions (e.g.
 * {@code @ExceptionHandler}).
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Michael Stummvoll
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ErrorAttributes
 * @see infra.app.config.web.ErrorProperties
 * @since 4.0
 */
@RequestMapping
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
  public Object error(HttpContext ctx) {
    return handleRequest(ctx, null);
  }

  @Override
  public void handleError(HttpContext http, @Nullable String message) {
    Object returnValue = handleRequest(http, message);
    if (returnValue != HttpRequestHandler.NONE_RETURN_VALUE) {
      try {
        returnValueHandler.handleReturnValue(http, null, returnValue);
      }
      catch (Throwable e) {
        throw ExceptionUtils.sneakyThrow(e);
      }
    }
  }

  private Object handleRequest(HttpContext http, @Nullable String message) {
    if (StringUtils.hasText(message)) {
      http.setAttribute(WebUtils.ERROR_MESSAGE_ATTRIBUTE, message);
    }

    HttpStatus status = getStatus(http);
    if (status.is2xxSuccessful()) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    boolean acceptsTextHtml = ifAcceptsTextHtml(http);
    Map<String, Object> model = getErrorAttributes(http, acceptsTextHtml ? MediaType.TEXT_HTML : MediaType.ALL);
    http.setStatus(status);
    if (acceptsTextHtml) {
      return resolveErrorView(http, status, model);
    }
    return model;
  }

  /**
   * Predicate that checks whether the current request explicitly support
   * {@code "text/html"} media type.
   * <p>
   * The "match-all" media type is not considered here.
   */
  static boolean ifAcceptsTextHtml(HttpContext context) {
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
  public ResponseEntity<String> mediaTypeNotAcceptable(HttpContext request) {
    HttpStatus status = getStatus(request);
    return ResponseEntity.status(status).build();
  }

}
