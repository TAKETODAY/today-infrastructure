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

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.interceptor.HandlerInterceptorsCapable;

/**
 * @author TODAY <br>
 *         2019-12-25 16:19
 */
public abstract class InterceptableRequestHandler implements RequestHandler, HandlerInterceptorsCapable {

    private static final Logger log = LoggerFactory.getLogger(InterceptableRequestHandler.class);

    protected boolean beforeProcess(final Object handler,
                                    final RequestContext context,
                                    final HandlerInterceptor[] interceptors) throws Throwable {

        for (final HandlerInterceptor intercepter : interceptors) {
            if (!intercepter.beforeProcess(context, handler)) {
                if (log.isDebugEnabled()) {
                    log.debug("Interceptor: [{}] return false", intercepter);
                }
                return false;
            }
        }
        return true;
    }

    protected void afterProcess(final Object result,
                                final Object handler,
                                final RequestContext context,
                                final HandlerInterceptor[] interceptors) throws Throwable {

        for (final HandlerInterceptor intercepter : interceptors) {
            intercepter.afterProcess(context, handler, result);
        }
    }

    @Override
    public Object handleRequest(final RequestContext context) throws Throwable {

        final HandlerInterceptor[] interceptors = getInterceptors();
        if (interceptors != null) {
            // before process
            if (!beforeProcess(this, context, interceptors)) {
                return null;
            }
            final Object result = handleInternal(context);

            afterProcess(result, this, context, interceptors);
            return result;
        }
        return handleInternal(context);
    }

    protected abstract Object handleInternal(RequestContext context) throws Throwable;

}
