/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.web.handler;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.PrintWriter;

import javax.imageio.ImageIO;

import cn.taketoday.core.OrderedSupport;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCapable;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.MediaType;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.view.ModelAndView;

/**
 * Simple {@link HandlerExceptionHandler}
 *
 * @author TODAY 2020-03-29 21:01
 */
public class SimpleExceptionHandler
        extends OrderedSupport implements HandlerExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(SimpleExceptionHandler.class);

  @Override
  public Object handleException(
          RequestContext context, Throwable target, Object handler) {
    logCatchThrowable(target);
    try {
      if (handler instanceof HandlerMethod) {
        return handleHandlerMethodInternal(target, context, (HandlerMethod) handler);
      }
      if (handler instanceof ViewController) {
        return handleViewControllerInternal(target, context, (ViewController) handler);
      }
      if (handler instanceof ResourceRequestHandler) {
        return handleResourceHandlerInternal(target, context, (ResourceRequestHandler) handler);
      }
      return handleExceptionInternal(target, context);
    }
    catch (Throwable handlerEx) {
      logResultedInException(target, handlerEx);
      // next in the chain
      return null;
    }
  }

  /**
   * record exception log occurred in target request handler
   *
   * @param target Throwable occurred in target request handler
   */
  protected void logCatchThrowable(Throwable target) {
    if (log.isDebugEnabled()) {
      log.debug("Catch Throwable: [{}]", target.toString(), target);
    }
  }

  /**
   * record log when an exception occurred in this exception handler
   *
   * @param target Throwable that occurred in request handler
   * @param handlerException Throwable occurred in this exception handler
   */
  protected void logResultedInException(Throwable target, Throwable handlerException) {
    log.error("Handling of [{}] resulted in Exception: [{}]",
            target.getClass().getName(),
            handlerException.getClass().getName(), handlerException);
  }

  /**
   * Resolve {@link ResourceRequestHandler} exception
   *
   * @param ex Target {@link Throwable}
   * @param context Current request context
   * @param handler {@link ResourceRequestHandler}
   * @throws Throwable If any {@link Exception} occurred
   */
  protected Object handleResourceHandlerInternal(
          Throwable ex, RequestContext context, ResourceRequestHandler handler) throws Throwable {
    return handleExceptionInternal(ex, context);
  }

  /**
   * Resolve {@link ViewController} exception
   *
   * @param ex Target {@link Throwable}
   * @param context Current request context
   * @param viewController {@link ViewController}
   * @throws Throwable If any {@link Exception} occurred
   */
  protected Object handleViewControllerInternal(
          Throwable ex, RequestContext context, ViewController viewController) throws Throwable {
    return handleExceptionInternal(ex, context);
  }

  /**
   * Resolve {@link HandlerMethod} exception
   *
   * @param ex Target {@link Throwable}
   * @param context Current request context
   * @param handlerMethod {@link HandlerMethod}
   * @throws Throwable If any {@link Exception} occurred
   */
  protected Object handleHandlerMethodInternal(
          Throwable ex, RequestContext context, HandlerMethod handlerMethod) throws Throwable//
  {
    context.setStatus(getErrorStatusValue(ex));

    if (handlerMethod.isReturnTypeAssignableTo(RenderedImage.class)) {
      return resolveImageException(ex, context);
    }
    if (!handlerMethod.isReturn(void.class)
            && !handlerMethod.isReturn(Object.class)
            && !handlerMethod.isReturn(ModelAndView.class)
            && !TemplateRendererReturnValueHandler.supportsHandlerMethod(handlerMethod)) {

      return handleExceptionInternal(ex, context);
    }

    writeErrorMessage(ex, context);
    return NONE_RETURN_VALUE;
  }

  /**
   * Write error message to request context, default is write json
   *
   * @param ex Throwable that occurred in request handler
   * @param context current request context
   */
  protected void writeErrorMessage(Throwable ex, RequestContext context) throws IOException {
    context.setContentType(MediaType.APPLICATION_JSON_VALUE);
    PrintWriter writer = context.getWriter();
    writer.write(buildDefaultErrorMessage(ex));
    writer.flush();
  }

  protected String buildDefaultErrorMessage(Throwable ex) {
    return new StringBuilder()
            .append("{\"message\":\"")
            .append(ex.getMessage())
            .append("\"}")
            .toString();
  }

  /**
   * Get error http status value, if target throwable is {@link HttpStatusCapable}
   * its return from {@link HttpStatusCapable#getHttpStatus()}
   *
   * @param ex Throwable that occurred in request handler
   * @return Http status code
   */
  public int getErrorStatusValue(Throwable ex) {
    if (ex instanceof HttpStatusCapable) { // @since 3.0.1
      HttpStatus httpStatus = ((HttpStatusCapable) ex).getHttpStatus();
      return httpStatus.value();
    }
    return HandlerMethod.getStatusValue(ex);
  }

  /**
   * resolve view exception
   *
   * @param ex Target {@link Exception}
   * @param context Current request context
   */
  public Object handleExceptionInternal(
          Throwable ex, RequestContext context) throws IOException {
    context.sendError(getErrorStatusValue(ex), ex.getMessage());
    return NONE_RETURN_VALUE;
  }

  /**
   * resolve image
   */
  public BufferedImage resolveImageException(
          Throwable ex, RequestContext context) throws IOException {
    ClassPathResource pathResource = new ClassPathResource("error/" + getErrorStatusValue(ex) + ".png");
    Assert.state(pathResource.exists(), "System Error");
    context.setContentType(MediaType.IMAGE_JPEG_VALUE);
    return ImageIO.read(pathResource.getInputStream());
  }

}
