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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.interceptor;

import cn.taketoday.web.RequestContext;

/**
 * Handler process around Handler.
 * 
 * @author TODAY <br>
 *         2018-06-25 20:06:11
 */
@FunctionalInterface
public interface HandlerInterceptor {

    /**
     * Before Handler process.
     * 
     * @param context
     *            Current request Context
     * @param handler
     *            Request handler
     * @return If is it possible to execute the target handler
     * @throws Throwable
     *             If any exception occurred
     */
    boolean beforeProcess(RequestContext context, Object handler) throws Throwable;

    /**
     * After Handler processed.
     * 
     * @param context
     *            Current request Context
     * @param handler
     *            Request handler
     * @param result
     *            Handler returned value
     * @throws Throwable
     *             If any exception occurred
     */
    default void afterProcess(RequestContext context, Object handler, Object result) throws Throwable {

    }
}
