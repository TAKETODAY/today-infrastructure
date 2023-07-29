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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import cn.taketoday.framework.web.error.AbstractErrorController;
import cn.taketoday.framework.web.error.ErrorAttributes;
import cn.taketoday.framework.web.error.ErrorProperties;
import cn.taketoday.framework.web.error.ErrorViewResolver;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.InvalidMediaTypeException;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.function.RequestPredicate;
import cn.taketoday.web.handler.function.RouterFunction;
import cn.taketoday.web.handler.function.RouterFunctions;
import cn.taketoday.web.handler.function.ServerRequest;
import cn.taketoday.web.handler.function.ServerResponse;
import cn.taketoday.web.view.ModelAndView;

import static cn.taketoday.web.handler.function.RequestPredicates.all;
import static cn.taketoday.web.handler.function.RouterFunctions.route;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/28 21:46
 */
public class DefaultSendErrorHandler extends AbstractErrorController implements SendErrorHandler {

  private static final MediaType TEXT_HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);

  final ReturnValueHandlerManager returnValueHandler;

  private final RouterFunction<ServerResponse> errorRouterFunction =
          route(acceptsTextHtml(), this::renderErrorView)
                  .andRoute(all(), this::renderErrorResponse);

  public DefaultSendErrorHandler(ErrorAttributes errorAttributes,
          ErrorProperties errorProperties, List<ErrorViewResolver> errorViewResolvers,
          ReturnValueHandlerManager returnValueHandler) {
    super(errorAttributes, errorProperties, errorViewResolvers);
    this.returnValueHandler = returnValueHandler;
  }

  @Override
  public void handleError(RequestContext context, @Nullable String message) {
    ServerRequest serverRequest = ServerRequest.create(context, returnValueHandler.getMessageConverters());
    errorRouterFunction.route(serverRequest)
            .ifPresent(handlerFunction -> {
              context.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE, serverRequest);
              try {
                Object returnValue = handlerFunction.handle(serverRequest)
                        .writeTo(context, serverRequest);
                if (returnValue != HttpRequestHandler.NONE_RETURN_VALUE) {
                  returnValueHandler.handleReturnValue(context, null, returnValue);
                }
              }
              catch (Throwable e) {
                throw ExceptionUtils.sneakyThrow(e);
              }
            });
  }

  /**
   * Predicate that checks whether the current request explicitly support
   * {@code "text/html"} media type.
   * <p>
   * The "match-all" media type is not considered here.
   *
   * @return the request predicate
   */
  protected RequestPredicate acceptsTextHtml() {
    return serverRequest -> {
      try {
        List<MediaType> acceptedMediaTypes = serverRequest.headers().accept();
        acceptedMediaTypes.removeIf(MediaType.ALL::equalsTypeAndSubtype);
        MimeTypeUtils.sortBySpecificity(acceptedMediaTypes);
        return acceptedMediaTypes.stream().anyMatch(MediaType.TEXT_HTML::isCompatibleWith);
      }
      catch (InvalidMediaTypeException ex) {
        return false;
      }
    };
  }

  /**
   * Render the error information as an HTML view.
   *
   * @param request the current request
   * @return a HTTP response
   */
  protected ServerResponse renderErrorView(ServerRequest request) {
    RequestContext context = request.requestContext();
    Map<String, Object> error = getErrorAttributes(context, MediaType.TEXT_HTML);

    int errorStatus = context.getStatus();
    ModelAndView errorView = resolveErrorView(context, HttpStatusCode.valueOf(context.getStatus()), error);
    return ServerResponse.status(errorStatus)
            .contentType(TEXT_HTML_UTF8)
            .render(errorView);
  }

  /**
   * Render the error information as a JSON payload.
   *
   * @param request the current request
   * @return a {@code Publisher} of the HTTP response
   */
  protected ServerResponse renderErrorResponse(ServerRequest request) {
    RequestContext requestedContext = request.requestContext();
    Map<String, Object> error = getErrorAttributes(requestedContext, MediaType.ALL);
    return ServerResponse.status(requestedContext.getStatus())
            .contentType(MediaType.APPLICATION_JSON)
            .body(error);
  }

}
