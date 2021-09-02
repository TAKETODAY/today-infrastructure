/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.view;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import cn.taketoday.core.Constant;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerMethod;

/**
 * @author TODAY 2019-07-14 17:41
 */
public class ObjectReturnValueHandler implements RuntimeReturnValueHandler, ReturnValueHandler {
  private final ResourceReturnValueHandler resourceHandler;
  private final ResponseBodyReturnValueHandler responseBodyHandler;
  private final RenderedImageReturnValueHandler renderedImageHandler;
  private final TemplateRendererReturnValueHandler templateRendererHandler;

  public ObjectReturnValueHandler(
          ResourceReturnValueHandler resourceHandler,
          ResponseBodyReturnValueHandler responseBodyHandler,
          RenderedImageReturnValueHandler renderedImageHandler,
          TemplateRendererReturnValueHandler templateRendererHandler) {
    this.resourceHandler = resourceHandler;
    this.responseBodyHandler = responseBodyHandler;
    this.renderedImageHandler = renderedImageHandler;
    this.templateRendererHandler = templateRendererHandler;
  }

  @Override
  public void handleReturnValue(
          RequestContext context, Object handler, Object returnValue) throws IOException {
    if (writeResponseBody(handler)) {// @since 3.0.5 fix response body error (github #16)
      writeResponseBody(context, returnValue);
    }
    else {
      handleObjectValue(context, returnValue);
    }
  }

  private void writeResponseBody(RequestContext context, Object returnValue) throws IOException {
    responseBodyHandler.write(context, returnValue);
  }

  public void handleObjectValue(final RequestContext request, final Object returnValue) throws IOException {
    if (returnValue instanceof String) {
      handleStringValue((String) returnValue, request);
    }
    else if (returnValue instanceof File) {
      resourceHandler.downloadFile((File) returnValue, request);
    }
    else if (returnValue instanceof Resource) {
      resourceHandler.downloadFile((Resource) returnValue, request);
    }
    else if (returnValue instanceof ModelAndView) {
      handleModelAndView(request, (ModelAndView) returnValue);
    }
    else if (returnValue instanceof RenderedImage) {
      handleImageValue((RenderedImage) returnValue, request);
    }
    else {
      writeResponseBody(request, returnValue);
    }
  }

  public void handleRedirect(final String redirect, final RequestContext context) throws IOException {
    if (StringUtils.isEmpty(redirect) || redirect.startsWith(Constant.HTTP)) {
      context.sendRedirect(redirect);
    }
    else {
      context.sendRedirect(context.getContextPath().concat(redirect));
    }
  }

  public void handleStringValue(final String resource, final RequestContext context) throws IOException {
    if (resource.startsWith(REDIRECT_URL_PREFIX)) {
      // redirect
      handleRedirect(resource.substring(9), context);
    }
    else if (resource.startsWith(RESPONSE_BODY_PREFIX)) {
      // body
      writeResponseBody(context, resource.substring(5));
    }
    else {
      // template view
      templateRendererHandler.render(resource, context);
    }
  }

  /**
   * Resolve {@link ModelAndView} return type
   *
   * @since 2.3.3
   */
  public void handleModelAndView(
          final RequestContext context, final @Nullable ModelAndView modelAndView) throws IOException {
    if (modelAndView != null && modelAndView.hasView()) {
      handleObjectValue(context, modelAndView.getView());
    }
  }

  /**
   * Resolve image
   *
   * @param context
   *         Current request context
   * @param image
   *         Image instance
   *
   * @throws IOException
   *         if an error occurs during writing.
   * @since 2.3.3
   */
  public void handleImageValue(final RenderedImage image, final RequestContext context) throws IOException {
    renderedImageHandler.write(image, context);
  }

  /**
   * determine this handler is write message to response body?
   *
   * @param handler
   *         target handler
   *
   * @since 3.0.3
   */
  protected boolean writeResponseBody(Object handler) {
    if (handler instanceof HandlerMethod) {
      return ((HandlerMethod) handler).isResponseBody();
    }
    return false;
  }

  @Override
  public boolean supportsReturnValue(Object returnValue) {
    return true;
  }

  @Override
  public boolean supportsHandler(Object handler) {
    return true;
  }

}
