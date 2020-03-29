/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.web.resolver;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.exception.ExceptionUnhandledException;
import cn.taketoday.web.handler.HandlerMethod;

/**
 * @author TODAY <br>
 *         2019-06-22 19:17
 * @since 2.3.7
 */
@Deprecated
public class ControllerAdviceExceptionResolver extends DefaultExceptionResolver implements WebApplicationInitializer {

    private static final Logger log = LoggerFactory.getLogger(ControllerAdviceExceptionResolver.class);
    private final Map<Class<? extends Throwable>, ThrowableHandlerMethod> exceptionHandlers = new HashMap<>();

    @Override
    protected void resolveHandlerMethodException(final Throwable ex,
                                                 final RequestContext context,
                                                 final HandlerMethod handlerMethod) throws Throwable //
    {

        final ThrowableHandlerMethod exceptionHandler = lookupExceptionHandler(ex);
        if (exceptionHandler != null) {
            context.attribute(Constant.KEY_THROWABLE, ex);
            if (handlerMethod.getObject() != null) { // apply status
                context.status(buildStatus(ex, exceptionHandler, handlerMethod).value());
            }
            try {
                exceptionHandler.handleResult(context, exceptionHandler.invokeHandler(context));
            }
            catch (final Throwable target) {
                if (target instanceof ExceptionUnhandledException == false) {
                    log.error("Handling of [{}] resulted in Exception: [{}]", //
                              target.getClass().getName(), target.getClass().getName(), target);
                    throw target;
                }
            }
        }
        else {
            super.resolveHandlerMethodException(ex, context, handlerMethod);
        }
    }

    /**
     * If target handler don't exist {@link ResponseStatus} it will look up at
     * exception handler
     * 
     * @param ex
     *            Target {@link Exception}
     * @param exceptionHandler
     *            Target exception handler
     * @param targetHandler
     *            Target handler
     * @return Current response status
     */
    protected ResponseStatus buildStatus(final Throwable ex,
                                         final ThrowableHandlerMethod exceptionHandler,
                                         final HandlerMethod targetHandler) //
    {
        // ResponseStatus on Target handler
        final DefaultResponseStatus status = super.buildStatus(targetHandler, ex);
        if (status.getOriginalStatus() == null) {
            return super.buildStatus(exceptionHandler, ex); // get ResponseStatus on exception handler
        }
        return status;
    }

    /**
     * Looking for exception handler mapping
     * 
     * @param ex
     *            Target {@link Exception}
     * @return Mapped {@link Exception} handler mapping
     */
    protected ThrowableHandlerMethod lookupExceptionHandler(final Throwable ex) {

        final ThrowableHandlerMethod ret = exceptionHandlers.get(ex.getClass());
        if (ret == null) {
            return exceptionHandlers.get(Throwable.class); // Global exception handler
        }
        return ret;
    }

    @Override
    public void onStartup(WebApplicationContext beanFactory) throws Throwable {

        log.info("Initialize controller advice exception resolver");

        // get all error handlers
        final List<Object> errorHandlers = beanFactory.getAnnotatedBeans(ControllerAdvice.class);

        for (final Object errorHandler : errorHandlers) {

            for (final Method method : errorHandler.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(ExceptionHandler.class)) {

                    final ThrowableHandlerMethod mapping = new ThrowableHandlerMethod(errorHandler, method);

                    for (Class<? extends Throwable> exceptionClass : method.getAnnotation(ExceptionHandler.class).value()) {
                        exceptionHandlers.put(exceptionClass, mapping);
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    protected static class ThrowableHandlerMethod extends HandlerMethod {

        public ThrowableHandlerMethod(Object handler, Method method) {
            super(handler, method, null);
        }
    }

}
