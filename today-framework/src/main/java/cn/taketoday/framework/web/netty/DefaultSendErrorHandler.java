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

package cn.taketoday.framework.web.netty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.framework.web.error.AbstractErrorController;
import cn.taketoday.framework.web.error.ErrorAttributes;
import cn.taketoday.framework.web.error.ErrorProperties;
import cn.taketoday.framework.web.error.ErrorViewResolver;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.InvalidMediaTypeException;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.ReturnValueHandlerManager;

/**
 * Default SendErrorHandler
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/28 21:46
 */
public class DefaultSendErrorHandler extends AbstractErrorController implements SendErrorHandler {

  private final ReturnValueHandlerManager returnValueHandler;

  public DefaultSendErrorHandler(ErrorAttributes errorAttributes, ErrorProperties errorProperties,
          List<ErrorViewResolver> errorViewResolvers, ReturnValueHandlerManager returnValueHandler) {
    super(errorAttributes, errorProperties, errorViewResolvers);
    Assert.notNull(returnValueHandler, "ReturnValueHandlerManager is required");
    this.returnValueHandler = returnValueHandler;
  }

  @Override
  public void handleError(RequestContext request, @Nullable String message) {
    Object returnValue = getReturnValue(request, message);
    if (returnValue != HttpRequestHandler.NONE_RETURN_VALUE) {
      try {
        returnValueHandler.handleReturnValue(request, null, returnValue);
      }
      catch (Throwable e) {
        throw ExceptionUtils.sneakyThrow(e);
      }
    }
  }

  private Object getReturnValue(RequestContext request, @Nullable String message) {
    if (ifAcceptsTextHtml(request)) {
      Map<String, Object> error = getErrorAttributes(request, MediaType.TEXT_HTML);
      if (message != null) {
        error.put("message", message);
      }
      request.setContentType("text/html;charset=UTF-8");
      return resolveErrorView(request, HttpStatusCode.valueOf(request.getStatus()), error);
    }
    else {
      request.setContentType(MediaType.APPLICATION_JSON);
      Map<String, Object> error = getErrorAttributes(request, MediaType.ALL);
      if (message != null) {
        error.put("message", message);
      }
      return error;
    }
  }

  /**
   * Predicate that checks whether the current request explicitly support
   * {@code "text/html"} media type.
   * <p>
   * The "match-all" media type is not considered here.
   */
  private static boolean ifAcceptsTextHtml(RequestContext context) {
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

}
