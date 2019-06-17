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
package cn.taketoday.web.servlet;

import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.web.Constant;
import cn.taketoday.web.annotation.WebDebugMode;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.mapping.HandlerInterceptorRegistry;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMappingRegistry;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.resolver.ParameterResolver;
import cn.taketoday.web.utils.WebUtils;
import cn.taketoday.web.view.ViewResolver;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today <br>
 * 
 *         2019-01-03 22:55
 */
@Slf4j
@WebDebugMode
@Singleton(Constant.DISPATCHER_SERVLET)
public class DebugDispatcherServlet extends DispatcherServlet {

    @Autowired
    public DebugDispatcherServlet(//
            ViewResolver viewResolver, //
            ParameterResolver parameterResolver, //
            ExceptionResolver exceptionResolver,
            HandlerMappingRegistry handlerMappingRegistry, //
            HandlerInterceptorRegistry handlerInterceptorRegistry) //
    {
        super(viewResolver, parameterResolver, exceptionResolver, handlerMappingRegistry, handlerInterceptorRegistry);
    }

    @Override
    public void service(final ServletRequest servletRequest, final ServletResponse servletResponse) //
            throws ServletException //
    {
        final long start = System.currentTimeMillis();

        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Find handler mapping
        final HandlerMapping requestMapping = lookupHandlerMapping(request);
        try {

            if (requestMapping == null) {
                response.sendError(404);
                return;
            }
            // Handler Method
            final Object result;
            final HandlerMethod handlerMethod = requestMapping.getHandlerMethod();
            if (requestMapping.hasInterceptor()) {
                // get intercepter s
                final int[] interceptors = requestMapping.getInterceptors();
                // invoke intercepter
                final HandlerInterceptorRegistry handlerInterceptorRegistry = getHandlerInterceptorRegistry();
                for (final int interceptor : interceptors) {
                    if (!handlerInterceptorRegistry.get(interceptor).beforeProcess(request, response, requestMapping)) {
                        log.debug("Before HandlerMethod Process: [{}] return false", handlerInterceptorRegistry.get(interceptor));
                        return;
                    }
                }
                result = invokeHandler(request, response, handlerMethod, requestMapping);
                for (final int interceptor : interceptors) {
                    HandlerInterceptor handlerInterceptor = handlerInterceptorRegistry.get(interceptor);
                    handlerInterceptor.afterProcess(result, request, response);
                    log.debug("After HandlerMethod Process: [{}]", handlerInterceptor);
                }
            }
            else {
                result = invokeHandler(request, response, handlerMethod, requestMapping);
            }

            resolveResult(request, response, handlerMethod, result);

            log.debug("Process [{}] takes: [{}]ms, with result: [{}]", //
                    request.getRequestURI(), (System.currentTimeMillis() - start), result);
        }
        catch (Throwable exception) {
            WebUtils.resolveException(request, response, //
                    getServletConfig().getServletContext(), getExceptionResolver(), requestMapping, exception);
        }
    }

    @Override
    protected Object invokeHandler(HttpServletRequest request, HttpServletResponse response, //
            HandlerMethod handlerMethod,
            HandlerMapping requestMapping) throws Throwable //
    {
        // method parameter
        final MethodParameter[] methodParameters = handlerMethod.getParameter();
        // Handler Method parameter list
        final Object[] args = new Object[methodParameters.length];

        getParameterResolver().resolveParameter(args, methodParameters, request, response);

        log.debug("Parameter list: {}", Arrays.toString(args));
        return handlerMethod.getMethod().invoke(requestMapping.getAction(), args); // invoke
    }

}
