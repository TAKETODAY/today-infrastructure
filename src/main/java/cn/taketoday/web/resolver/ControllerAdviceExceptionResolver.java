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
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.config.ActionConfiguration;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.WebMapping;

/**
 * @author TODAY <br>
 *         2019-06-22 19:17
 * @since 2.3.7
 */
@MissingBean
@Singleton(Constant.EXCEPTION_RESOLVER)
public class ControllerAdviceExceptionResolver extends DefaultExceptionResolver implements BeanFactoryAware {

    private final Map<Class<? extends Throwable>, ExceptionHandlerMapping> exceptionHandlers = new HashMap<>();

    @Override
    public void resolveException(RequestContext requestContext, Throwable ex, WebMapping mvcMapping) throws Throwable {

        if (mvcMapping instanceof HandlerMapping) {
            final ExceptionHandlerMapping exceptionHandler = exceptionHandlers.get(ex.getClass());
            if (exceptionHandler != null) {
                final Object result = invokeExceptionHandler(requestContext, exceptionHandler);
                exceptionHandler.resolveResult(requestContext, result);
                return;
            }
        }
        super.resolveException(requestContext, ex, mvcMapping);
    }

    protected Object invokeExceptionHandler(//
            final RequestContext request, final ExceptionHandlerMapping handler) throws Throwable //
    {
        // Handler Method parameter list
        return handler.getMethod()//
                .invoke(handler.getHandler(), handler.resolveParameters(request)); // invoke
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {

        final List<Object> handlers = beanFactory.getAnnotatedBeans(ControllerAdvice.class);

        for (final Object handler : handlers) {

            final Class<? extends Object> handlerClass = handler.getClass();

            for (final Method method : handlerClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(ExceptionHandler.class)) {

                    final HandlerMethod handlerMethod = //
                            ActionConfiguration.createHandlerMethod(method, ActionConfiguration.createMethodParameters(method));

                    final Object bean = beanFactory.getBean(handlerClass);

                    final ExceptionHandlerMapping mapping = new ExceptionHandlerMapping(bean, handlerMethod);

                    for (Class<? extends Throwable> value : method.getAnnotation(ExceptionHandler.class).value()) {
                        exceptionHandlers.put(value, mapping);
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    private static class ExceptionHandlerMapping extends HandlerMethod implements Serializable {

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
