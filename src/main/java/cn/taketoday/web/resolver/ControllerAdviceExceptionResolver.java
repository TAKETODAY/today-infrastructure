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
package cn.taketoday.web.resolver;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.config.ActionConfiguration;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.exception.ExceptionUnhandledException;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.WebMapping;

/**
 * @author TODAY <br>
 *         2019-06-22 19:17
 * @since 2.3.7
 */
@MissingBean(value = Constant.EXCEPTION_RESOLVER, type = ExceptionResolver.class)
public class ControllerAdviceExceptionResolver extends DefaultExceptionResolver implements WebApplicationInitializer {

    private static final Logger log = LoggerFactory.getLogger(ControllerAdviceExceptionResolver.class);
    private final Map<Class<? extends Throwable>, ExceptionHandlerMapping> exceptionHandlers = new HashMap<>();

    @Override
    public void resolveException(final RequestContext requestContext, //
            final Throwable ex, final WebMapping mvcMapping) throws Throwable //
    {

        if (mvcMapping instanceof HandlerMapping) {

            final ExceptionHandlerMapping exceptionHandler = lookupExceptionHandlerMapping(ex);//
            if (exceptionHandler != null) {
                requestContext.attribute(Constant.KEY_THROWABLE, ex);
                final HandlerMethod handlerMethod = ((HandlerMapping) mvcMapping).getHandlerMethod();

                if (handlerMethod != null) { //
                    requestContext.status(buildStatus(ex, exceptionHandler, handlerMethod).value());
                }
                try {
                    exceptionHandler.resolveResult(requestContext, invokeExceptionHandler(requestContext, exceptionHandler));
                    log.error("Catch Throwable: [{}] With Msg: [{}]", ex, ex.getMessage(), ex);
                    return;
                }
                catch (final InvocationTargetException e) {
                    final Throwable target = e.getTargetException();
                    if (target instanceof ExceptionUnhandledException == false) {
                        log.error("Handling of [{}] resulted in Exception: [{}]", //
                                target.getClass().getName(), target.getClass().getName(), target);
                        throw target;
                    }
                }
            }
        }
        super.resolveException(requestContext, ex, mvcMapping);
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
    protected ResponseStatus buildStatus(final Throwable ex, //
            final ExceptionHandlerMapping exceptionHandler, final HandlerMethod targetHandler) //
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
    protected ExceptionHandlerMapping lookupExceptionHandlerMapping(Throwable ex) {

        final ExceptionHandlerMapping ret = exceptionHandlers.get(ex.getClass());
        if (ret == null) {
            return exceptionHandlers.get(Throwable.class); // Global exception handler
        }
        return ret;
    }

    /**
     * Invoke {@link Exception} handler
     * 
     * @param request
     *            Current request context
     * @param handler
     *            {@link Exception} handler
     * @return {@link Exception} handler result
     * @throws Throwable
     *             If any {@link Exception} occurred when resolve {@link Exception}
     *             handler parameter or {@link Exception} handler throw an
     *             {@link Exception}
     */
    protected Object invokeExceptionHandler(//
            final RequestContext request, final ExceptionHandlerMapping handler) throws Throwable //
    {
        // Handler Method parameter list
        return handler.getMethod()//
                .invoke(handler.getHandler(), handler.resolveParameters(request)); // invoke
    }

    @Override
    public void onStartup(WebApplicationContext beanFactory) throws Throwable {

        LoggerFactory.getLogger(getClass()).info("Initialize controller advice exception resolver");

        // get all error handlers
        final List<Object> errorHandlers = beanFactory.getAnnotatedBeans(ControllerAdvice.class);

        for (final Object handler : errorHandlers) {

            final Class<? extends Object> handlerClass = handler.getClass();

            for (final Method method : handlerClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(ExceptionHandler.class)) {

                    final HandlerMethod handlerMethod = //
                            HandlerMethod.create(method, ActionConfiguration.createMethodParameters(method));

                    final Object errorHandlerBean = beanFactory.getBean(handlerClass);

                    final ExceptionHandlerMapping mapping = new ExceptionHandlerMapping(errorHandlerBean, handlerMethod);

                    for (Class<? extends Throwable> exceptionClass : method.getAnnotation(ExceptionHandler.class).value()) {
                        exceptionHandlers.put(exceptionClass, mapping);
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    protected static class ExceptionHandlerMapping extends HandlerMethod implements Serializable {

        private final Object handler;

        public ExceptionHandlerMapping(Object handler, HandlerMethod handlerMethod) {
            super(handlerMethod.getMethod(), handlerMethod.getReutrnType(), handlerMethod.getParameters());
            this.handler = handler;
        }

        public Object getHandler() {
            return handler;
        }
    }

}
