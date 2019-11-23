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
package cn.taketoday.web.servlet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.taketoday.context.ApplicationContext.State;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.mapping.HandlerMapping;

/**
 * @author TODAY <br>
 *         2019-11-16 19:05
 */
public class DispatcherHandler {

    private static final Logger log = LoggerFactory.getLogger(DispatcherHandler.class);

    public static void service(final HandlerMapping mapping, final RequestContext context) throws Throwable {

        final Object result;
        // Handler Method
        if (mapping.hasInterceptor()) {
            // get intercepter s
            final HandlerInterceptor[] interceptors = mapping.getInterceptors();
            // invoke intercepter
            for (final HandlerInterceptor intercepter : interceptors) {
                if (!intercepter.beforeProcess(context, mapping)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Interceptor: [{}] return false", intercepter);
                    }
                    return;
                }
            }
            result = mapping.invokeHandler(context);
            for (final HandlerInterceptor intercepter : interceptors) {
                intercepter.afterProcess(context, mapping, result);
            }
        }
        else {
            result = mapping.invokeHandler(context);
        }
        mapping.resolveResult(context, result);
    }

    public static void destroy(WebApplicationContext applicationContext) {

        if (applicationContext != null) {
            final State state = applicationContext.getState();

            if (state != State.CLOSING && state != State.CLOSED) {

                applicationContext.close();
                final DateFormat dateFormat = new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT);//
                final String msg = new StringBuffer()//
                        .append("Your application destroyed at: [")//
                        .append(dateFormat.format(new Date()))//
                        .append("] on startup date: [")//
                        .append(dateFormat.format(applicationContext.getStartupDate()))//
                        .append("]")//
                        .toString();

                log.info(msg);
            }
        }
    }
}
