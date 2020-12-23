/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
import java.net.URL;

import javax.imageio.ImageIO;

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.exception.ExceptionUnhandledException;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.utils.WebUtils;
import cn.taketoday.web.view.TemplateResultHandler;

/**
 * @author TODAY <br>
 * 2020-03-29 21:01
 */
public class DefaultExceptionHandler implements HandlerExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(DefaultExceptionHandler.class);

  @Override
  public Object handleException(final RequestContext context,
                                final Throwable ex, final Object handler) throws Throwable {
    try {
      if (handler instanceof HandlerMethod) {
        return handleHandlerMethodInternal(ex, context, (HandlerMethod) handler);
      }
      if (handler instanceof ViewController) {
        return handleViewControllerInternal(ex, context, (ViewController) handler);
      }
      if (handler instanceof ResourceRequestHandler) {
        return handleResourceMappingInternal(ex, context, (ResourceRequestHandler) handler);
      }

      if (log.isDebugEnabled()) {
        log.debug("Catch Throwable: [{}]", ex.toString(), ex);
      }
      return handleExceptionInternal(ex, context);
    }
    catch (ExceptionUnhandledException unhandled) {
      throw unhandled;
    }
    catch (Throwable handlerException) {
      log.error("Handling of [{}] resulted in Exception: [{}]", //
                ex.getClass().getName(), handlerException.getClass().getName(), handlerException);

      throw handlerException;
    }
  }

  /**
   * Resolve {@link ResourceRequestHandler} exception
   *
   * @param ex
   *         Target {@link Throwable}
   * @param context
   *         Current request context
   * @param handler
   *         {@link ResourceRequestHandler}
   *
   * @throws Throwable
   *         If any {@link Exception} occurred
   */
  protected Object handleResourceMappingInternal(final Throwable ex,
                                                 final RequestContext context,
                                                 final ResourceRequestHandler handler) throws Throwable {
    return handleExceptionInternal(ex, context);
  }

  /**
   * Resolve {@link ViewController} exception
   *
   * @param ex
   *         Target {@link Throwable}
   * @param context
   *         Current request context
   * @param viewController
   *         {@link ViewController}
   *
   * @throws Throwable
   *         If any {@link Exception} occurred
   */
  protected Object handleViewControllerInternal(final Throwable ex,
                                                final RequestContext context,
                                                final ViewController viewController) throws Throwable {
    return handleExceptionInternal(ex, context);
  }

  /**
   * Resolve {@link HandlerMethod} exception
   *
   * @param ex
   *         Target {@link Throwable}
   * @param context
   *         Current request context
   * @param handlerMethod
   *         {@link HandlerMethod}
   *
   * @throws Throwable
   *         If any {@link Exception} occurred
   */
  protected Object handleHandlerMethodInternal(final Throwable ex,
                                               final RequestContext context,
                                               final HandlerMethod handlerMethod) throws Throwable//
  {
    context.status(getErrorStatusValue(ex));

    if (handlerMethod.isAssignableFrom(RenderedImage.class)) {
      return resolveImageException(ex, context);
    }
    if (!handlerMethod.is(void.class)
            && !handlerMethod.is(Object.class)
            && !handlerMethod.is(ModelAndView.class)
            && TemplateResultHandler.supportsHandlerMethod(handlerMethod)) {

      return handleExceptionInternal(ex, context);
    }

    context.contentType(Constant.CONTENT_TYPE_JSON);
    final PrintWriter writer = context.getWriter();
    writer.write(buildDefaultErrorMessage(ex));
    writer.flush();
    return NONE_RETURN_VALUE;
  }

  protected String buildDefaultErrorMessage(final Throwable ex) {
    return new StringBuilder()
            .append("{\"message\":\"")
            .append(ex.getMessage())
            .append("\"}")
            .toString();
  }

  public int getErrorStatusValue(Throwable ex) {
    return WebUtils.getStatusValue(ex);
  }

  /**
   * resolve view exception
   *
   * @param ex
   *         Target {@link Exception}
   * @param context
   *         Current request context
   */
  public Object handleExceptionInternal(final Throwable ex, final RequestContext context) throws IOException {
    context.sendError(getErrorStatusValue(ex), ex.getMessage());
    return NONE_RETURN_VALUE;
  }

  /**
   * resolve image
   */
  public BufferedImage resolveImageException(final Throwable ex, final RequestContext context) throws IOException {
    final URL resource = ClassUtils.getClassLoader()
            .getResource(new StringBuilder()
                                 .append("/error/")
                                 .append(getErrorStatusValue(ex))
                                 .append(".png").toString());

    Assert.state(resource != null, "System Error");

    context.contentType(Constant.CONTENT_TYPE_IMAGE);
    return ImageIO.read(resource);
  }

}
