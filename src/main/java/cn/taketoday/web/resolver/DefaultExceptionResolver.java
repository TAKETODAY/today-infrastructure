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

import java.io.IOException;
import java.lang.reflect.Method;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.WebDebugMode;
import cn.taketoday.web.exception.AccessForbiddenException;
import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.exception.FileSizeExceededException;
import cn.taketoday.web.exception.MethodNotAllowedException;
import cn.taketoday.web.exception.NotFoundException;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.WebMapping;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author TODAY <br>
 *         2018-06-25 20:27:22
 */
@Slf4j
@WebDebugMode
@Singleton(Constant.EXCEPTION_RESOLVER)
public class DefaultExceptionResolver implements ExceptionResolver {

    @Override
    public void resolveException(HttpServletRequest request, //
            HttpServletResponse response, Throwable ex, WebMapping webMapping) throws Throwable //
    {
        try {

            response.reset();
            if (webMapping instanceof HandlerMapping) {

                HandlerMapping handlerMapping = (HandlerMapping) webMapping;

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
                response.setStatus(status);
                switch (handlerMethod.getReutrnType())
                {
                    case Constant.RETURN_FILE :
                    case Constant.RETURN_VOID :
                    case Constant.RETURN_JSON :
                    case Constant.RETURN_STRING :

                        response.setContentType(Constant.CONTENT_TYPE_JSON);

                        response.getWriter().print(new StringBuilder()//
                                .append("{\"msg\":\"").append(msg)//
                                .append("\",\"code\":").append(status)//
                                .append(",\"success\":false}")//
                                .toString()//
                        );

                        break;
                    case Constant.RETURN_IMAGE :
                        resolveImageException(ex, response);
                        break;

                    default:
                    case Constant.RETURN_VIEW :
                    case Constant.RETURN_OBJECT :
                    case Constant.RETURN_MODEL_AND_VIEW :
                        if (StringUtils.isNotEmpty(redirect)) {
                            response.sendRedirect(request.getContextPath() + redirect);
                        }
                        else
                            resolveViewException(ex, response, status, msg);
                        break;
                }
            }
            else {
                resolveViewException(ex, response, 500, ex.getMessage());
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
            return HttpServletResponse.SC_METHOD_NOT_ALLOWED;
        }
        else if (ex instanceof BadRequestException || //
                ex instanceof ConversionException || //
                ex instanceof FileSizeExceededException) //
        {
            return HttpServletResponse.SC_BAD_REQUEST;
        }
        else if (ex instanceof NotFoundException) {
            return HttpServletResponse.SC_NOT_FOUND;
        }
        else if (ex instanceof AccessForbiddenException) {
            return HttpServletResponse.SC_FORBIDDEN;
        }
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    /**
     * resolve view exception
     * 
     * @param ex
     * @param response
     * @param status
     * @param msg
     * @throws IOException
     */
    public static void resolveViewException(Throwable ex, //
            HttpServletResponse response, int status, String msg) throws IOException //
    {
        if (ex instanceof MethodNotAllowedException) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
        }
        else if (ex instanceof BadRequestException || //
                ex instanceof ConversionException || //
                ex instanceof FileSizeExceededException) //
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        }
        else if (ex instanceof NotFoundException) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
        }
        else if (ex instanceof AccessForbiddenException) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
        }
        else {
            response.sendError(status, msg);
        }
    }

    /**
     * resolve image
     * 
     * @param ex
     * @param response
     * @throws IOException
     */
    public static void resolveImageException(Throwable ex, HttpServletResponse response) throws IOException {
        response.setContentType(Constant.CONTENT_TYPE_IMAGE);
        String fileName = "/error/500.png";
        if (ex instanceof MethodNotAllowedException) {
            fileName = "/error/405.png";
        } //
        else if (ex instanceof BadRequestException || ex instanceof ConversionException) {
            fileName = "/error/400.png";
        } //
        else if (ex instanceof NotFoundException) {
            fileName = "/error/404.png";
        } //
        else if (ex instanceof AccessForbiddenException) {
            fileName = "/error/403.png";
        }
        ImageIO.write(//
                ImageIO.read(ClassUtils.getClassLoader().getResource(fileName)), //
                Constant.IMAGE_PNG, //
                response.getOutputStream()//
        );
    }

}
