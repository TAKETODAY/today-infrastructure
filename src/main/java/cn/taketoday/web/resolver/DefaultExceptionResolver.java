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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.resolver;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.lang.annotation.Annotation;

import javax.imageio.ImageIO;

import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.exception.AccessForbiddenException;
import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.exception.FileSizeExceededException;
import cn.taketoday.web.exception.MethodNotAllowedException;
import cn.taketoday.web.exception.NotFoundException;
import cn.taketoday.web.exception.UnauthorizedException;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.ResourceMapping;
import cn.taketoday.web.mapping.ViewMapping;
import cn.taketoday.web.mapping.WebMapping;
import cn.taketoday.web.resolver.result.ViewResolverResultResolver;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation
 * 
 * @author TODAY <br>
 *         2018-06-25 20:27:22
 */
@Slf4j
public class DefaultExceptionResolver implements ExceptionResolver {

    @Override
    public void resolveException(final RequestContext context,
                                 final Throwable ex, final WebMapping mvcMapping) throws Throwable //
    {
        try {

            if (mvcMapping == null) {
                resolveViewException(ex, context, ex.getMessage());
            }
            else if (mvcMapping instanceof HandlerMapping) {
                resolveHandlerMappingException(ex, context, (HandlerMapping) mvcMapping);
            }
            else if (mvcMapping instanceof ViewMapping) {
                resolveViewMappingException(ex, context, (ViewMapping) mvcMapping);
            }
            else if (mvcMapping instanceof ResourceMapping) {
                resolveResourceMappingException(ex, context, (ResourceMapping) mvcMapping);
            }
            log.debug("Catch Throwable: [{}]", ex.toString(), ex);
        }
        catch (Throwable handlerException) {
            log.error("Handling of [{}] resulted in Exception: [{}]", //
                      ex.getClass().getName(), handlerException.getClass().getName(), handlerException);

            throw handlerException;
        }
    }

    /**
     * Resolve {@link HandlerMapping} exception
     * 
     * @param ex
     *            Target {@link Throwable}
     * @param context
     *            Current request context
     * @param resourceMapping
     *            {@link ResourceMapping}
     * @throws Throwable
     *             If any {@link Exception} occurred
     */
    protected void resolveResourceMappingException(final Throwable ex,
                                                   final RequestContext context,
                                                   final ResourceMapping resourceMapping) throws Throwable {
        resolveViewException(ex, context, ex.getMessage());
    }

    /**
     * Resolve {@link HandlerMapping} exception
     * 
     * @param ex
     *            Target {@link Throwable}
     * @param context
     *            Current request context
     * @param viewMapping
     *            {@link ViewMapping}
     * @throws Throwable
     *             If any {@link Exception} occurred
     */
    protected void resolveViewMappingException(final Throwable ex,
                                               final RequestContext context,
                                               final ViewMapping viewMapping) throws Throwable {
        resolveViewException(ex, context, ex.getMessage());
    }

    /**
     * Resolve {@link HandlerMapping} exception
     * 
     * @param ex
     *            Target {@link Throwable}
     * @param context
     *            Current request context
     * @param handlerMapping
     *            {@link HandlerMapping}
     * @throws Throwable
     *             If any {@link Exception} occurred
     */
    protected void resolveHandlerMappingException(final Throwable ex,
                                                  final RequestContext context,
                                                  final HandlerMapping handlerMapping) throws Throwable//
    {
        final HandlerMethod handlerMethod = ((HandlerMethod) handlerMapping);

        final ResponseStatus responseStatus = buildStatus(handlerMethod, ex);
        final int status = responseStatus.value();

        context.status(status);

        if (handlerMethod.isAssignableFrom(RenderedImage.class)) {
            handlerMethod.resolveResult(context, resolveImageException(ex, context));
        }
        else if (!handlerMethod.is(void.class)
                 && !handlerMethod.is(Object.class)
                 && !handlerMethod.is(ModelAndView.class)
                 && ViewResolverResultResolver.supportsResolver(handlerMethod)) {

            final String redirect = responseStatus.redirect();
            if (StringUtils.isNotEmpty(redirect)) { // has redirect
                context.redirect(context.contextPath() + redirect);
            }
            else {
                resolveViewException(ex, context, ex.getMessage());
            }
        }
        else {

            context.contentType(Constant.CONTENT_TYPE_JSON);
            context.getWriter().write(new StringBuilder()
                    .append("{\"message\":\"").append(responseStatus.msg())
                    .append("\",\"status\":").append(status)
                    .append(",\"success\":false}")
                    .toString()//
            );
        }
    }

    public static int getStatus(Throwable ex) {

        if (ex instanceof MethodNotAllowedException) {
            return 405;
        }
        else if (ex instanceof BadRequestException
                 || ex instanceof ValidationException
                 || ex instanceof ConversionException
                 || ex instanceof FileSizeExceededException) //
        {
            return 400;
        }
        else if (ex instanceof NotFoundException) {
            return 404;
        }
        else if (ex instanceof UnauthorizedException) {
            return 401;
        }
        else if (ex instanceof AccessForbiddenException) {
            return 403;
        }
        return 500;
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
    public static void resolveViewException(final Throwable ex,
                                            final RequestContext context, String msg) throws IOException {
        context.sendError(getStatus(ex), msg);
    }

    /**
     * resolve image
     */
    public static BufferedImage resolveImageException(final Throwable ex,
                                                      final RequestContext context) throws IOException {

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

        ResponseStatus responseStatus = handlerMethod.getMethodAnnotation(ResponseStatus.class);
        if (responseStatus == null) {
            responseStatus = handlerMethod.getDeclaringClassAnnotation(ResponseStatus.class);
        }
        return new DefaultResponseStatus(responseStatus, ex);
    }

    @SuppressWarnings("all")
    protected static class DefaultResponseStatus implements ResponseStatus {

        private final Throwable ex;
        private final ResponseStatus status;

        public DefaultResponseStatus(ResponseStatus status, Throwable ex) {
            this.status = status;
            this.ex = ex;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return ResponseStatus.class;
        }

        @Override
        public int value() {

            if (status == null) {
                return getStatus(ex);
            }

            final int value = status.value();
            return value == 0 ? getStatus(ex) : value;
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
