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
import java.lang.annotation.Annotation;

import javax.imageio.ImageIO;

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.utils.WebUtils;
import cn.taketoday.web.view.TemplateResultHandler;

/**
 * @author TODAY <br>
 *         2020-03-29 21:01
 */
public class DefaultExceptionHandler implements HandlerExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultExceptionHandler.class);

    @Override
    public void handleException(final RequestContext context, final Throwable ex, final Object handler) throws Throwable {

        try {
        
            if (handler instanceof HandlerMethod) {
                handleHandlerMethodInternal(ex, context, (HandlerMethod) handler);
            }
            else if (handler instanceof ViewController) {
                handleViewControllerInternal(ex, context, (ViewController) handler);
            }
            else if (handler instanceof ResourceRequestHandler) {
                handleResourceMappingInternal(ex, context, (ResourceRequestHandler) handler);
            }
            else {
                handleExceptionInternal(ex, context);
            }

            if (log.isDebugEnabled()) {
                log.debug("Catch Throwable: [{}]", ex.toString(), ex);
            }
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
     *            Target {@link Throwable}
     * @param context
     *            Current request context
     * @param resourceRequestHandler
     *            {@link ResourceRequestHandler}
     * @throws Throwable
     *             If any {@link Exception} occurred
     */
    protected void handleResourceMappingInternal(final Throwable ex,
                                                 final RequestContext context,
                                                 final ResourceRequestHandler resourceRequestHandler) throws Throwable {
        handleExceptionInternal(ex, context);
    }

    /**
     * Resolve {@link ViewController} exception
     * 
     * @param ex
     *            Target {@link Throwable}
     * @param context
     *            Current request context
     * @param viewController
     *            {@link ViewController}
     * @throws Throwable
     *             If any {@link Exception} occurred
     */
    protected void handleViewControllerInternal(final Throwable ex,
                                                final RequestContext context,
                                                final ViewController viewController) throws Throwable {
        handleExceptionInternal(ex, context);
    }

    /**
     * Resolve {@link HandlerMethod} exception
     * 
     * @param ex
     *            Target {@link Throwable}
     * @param context
     *            Current request context
     * @param handlerMethod
     *            {@link HandlerMethod}
     * @throws Throwable
     *             If any {@link Exception} occurred
     */
    protected void handleHandlerMethodInternal(final Throwable ex,
                                               final RequestContext context,
                                               final HandlerMethod handlerMethod) throws Throwable//
    {
        final ResponseStatus responseStatus = buildStatus(handlerMethod, ex);
        final int status = responseStatus.value();

        context.status(status);

        if (handlerMethod.isAssignableFrom(RenderedImage.class)) {
            handlerMethod.handleResult(context, resolveImageException(ex, context));
        }
        else if (!handlerMethod.is(void.class)
                 && !handlerMethod.is(Object.class)
                 && !handlerMethod.is(ModelAndView.class)
                 && TemplateResultHandler.supportsHandlerMethod(handlerMethod)) {

            final String redirect = responseStatus.redirect();
            if (StringUtils.isNotEmpty(redirect)) { // has redirect
                context.redirect(context.contextPath().concat(redirect));
            }
            else {
                handleExceptionInternal(ex, context);
            }
        }
        else {
            context.contentType(Constant.CONTENT_TYPE_JSON);
            final PrintWriter writer = context.getWriter();
            writer.write(new StringBuilder()
                    .append("{\"message\":\"").append(responseStatus.msg())
                    .append("\",\"status\":").append(status)
                    .append(",\"success\":false}")
                    .toString()//
            );
            writer.flush();
        }
    }

    public int getStatus(Throwable ex) {
        return WebUtils.getStatus(ex);
    }

    /**
     * resolve view exception
     * 
     * @param ex
     *            Target {@link Exception}
     * @param context
     *            Current request context
     * @param msg
     *            Message to client
     */
    public void handleExceptionInternal(final Throwable ex, final RequestContext context) throws IOException {
        context.sendError(getStatus(ex), ex.getMessage());
    }

    /**
     * resolve image
     */
    public BufferedImage resolveImageException(final Throwable ex, final RequestContext context) throws IOException {

        context.contentType(Constant.CONTENT_TYPE_IMAGE);

        return ImageIO.read(ClassUtils.getClassLoader().getResource(new StringBuilder()//
                .append("/error/")//
                .append(getStatus(ex))//
                .append(".png").toString())//
        );
    }

    /**
     * Build a {@link ResponseStatus} from target {@link HandlerMethod}
     * 
     * @param handlerMethod
     *            Target handler method
     * @param ex
     *            Current {@link Exception}
     * @return {@link DefaultResponseStatus}
     */
    protected DefaultResponseStatus buildStatus(final HandlerMethod handlerMethod, final Throwable ex) {

        ResponseStatus responseStatus = handlerMethod.getStatus();
        if (responseStatus == null) {
            responseStatus = ex.getClass().getAnnotation(ResponseStatus.class);
        }
        return new DefaultResponseStatus(responseStatus, ex);
    }

    @SuppressWarnings("all")
    protected static class DefaultResponseStatus implements ResponseStatus {

        private final Throwable ex;
        private final ResponseStatus status;

        public DefaultResponseStatus(ResponseStatus status, Throwable ex) {
            this.ex = ex;
            this.status = status;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return ResponseStatus.class;
        }

        @Override
        public int value() {

            if (status == null) {
                return WebUtils.getStatus(ex);
            }

            final int value = status.value();
            return value == 0 ? WebUtils.getStatus(ex) : value;
        }

        @Override
        public String msg() {
            if (status == null) {
                return ex.getMessage();
            }
            return status.msg();
        }

        @Override
        public String redirect() {
            if (status == null) {
                return null;
            }
            return status.redirect();
        }

        public final ResponseStatus getOriginalStatus() {
            return status;
        }

    }
}
