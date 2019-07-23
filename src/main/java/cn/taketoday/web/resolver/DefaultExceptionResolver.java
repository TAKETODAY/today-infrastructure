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
import java.lang.reflect.Method;

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
import cn.taketoday.web.mapping.WebMapping;
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
    public void resolveException(RequestContext requestContext, //
            Throwable ex, WebMapping mvcMapping) throws Throwable //
    {
        try {

            requestContext.reset();
            if (mvcMapping instanceof HandlerMapping) {

                HandlerMapping handlerMapping = (HandlerMapping) mvcMapping;

                final HandlerMethod handlerMethod = handlerMapping.getHandlerMethod();
                final Method method = handlerMethod.getMethod();
                ResponseStatus responseStatus = method.getAnnotation(ResponseStatus.class);
                if (responseStatus == null) {
                    responseStatus = method.getDeclaringClass().getAnnotation(ResponseStatus.class);
                }
                int status = getStatus(ex);
                String msg = ex.getMessage();
                String redirect = null;
                if (responseStatus != null) {
                    msg = responseStatus.msg();
                    int value = responseStatus.value();
                    status = value == 0 ? status : value;
                    redirect = responseStatus.redirect();
                }

                requestContext.status(status);

                if (handlerMethod.isAssignableFrom(RenderedImage.class)) {
                    handlerMethod.resolveResult(requestContext, resolveImageException(ex, requestContext));
                }
                else if (handlerMethod.is(void.class) //
                        || handlerMethod.is(Object.class)//
                        || handlerMethod.is(ModelAndView.class)) {
                    // has redirect
                    if (StringUtils.isNotEmpty(redirect)) {
                        requestContext.redirect(requestContext.contextPath() + redirect);
                    }
                    else {
                        resolveViewException(ex, requestContext, ex.getMessage());
                    }
                }
                else {

                    requestContext.contentType(Constant.CONTENT_TYPE_JSON);
                    requestContext.getWriter().write(new StringBuilder()//
                            .append("{\"msg\":\"").append(msg)//
                            .append("\",\"status\":").append(status)//
                            .append(",\"success\":false}")//
                            .toString()//
                    );
                }
            }
            else {
                resolveViewException(ex, requestContext, ex.getMessage());
            }

            log.error("Catch Throwable: [{}] With Msg: [{}]", ex, ex.getMessage(), ex);
        }
        catch (Throwable handlerException) {
            log.error("Handling of [{}] resulted in Exception: [{}]", //
                    ex.getClass().getName(), handlerException.getClass().getName(), handlerException);

            throw handlerException;
        }
    }

    public static int getStatus(Throwable ex) {

        if (ex instanceof MethodNotAllowedException) {
            return 405;
        }
        else if (ex instanceof BadRequestException || //
                ex instanceof ValidationException || //
                ex instanceof ConversionException || //
                ex instanceof FileSizeExceededException) //
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
     * @param requestContext
     *            Current request context
     * @param msg
     */
    public static void resolveViewException(Throwable ex, //
            final RequestContext requestContext, String msg) throws IOException //
    {
        requestContext.sendError(getStatus(ex), msg);
    }

    /**
     * resolve image
     */
    public static BufferedImage resolveImageException(final Throwable ex, final RequestContext requestContext) throws IOException {

        requestContext.contentType(Constant.CONTENT_TYPE_IMAGE);

        return ImageIO.read(ClassUtils.getClassLoader().getResource(new StringBuilder()//
                .append("/error/")//
                .append(getStatus(ex))//
                .append(".png").toString())//
        );
    }

}
