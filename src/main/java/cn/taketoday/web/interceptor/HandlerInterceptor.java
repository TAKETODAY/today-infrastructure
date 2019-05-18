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
package cn.taketoday.web.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMethod;

/**
 * Handler Intercepter process around Handler.
 * 
 * @author Today <br>
 * 
 *         2018-06-25 20:06:11
 */
@FunctionalInterface
public interface HandlerInterceptor {

    /**
     * Before {@link HandlerMethod} process.
     * 
     * @param request
     *            request
     * @param response
     *            response
     * @param handlerMapping
     *            request mapping
     * @return
     * @throws Throwable
     */
    boolean beforeProcess(HttpServletRequest request, HttpServletResponse response, HandlerMapping handlerMapping)
            throws Throwable;

    /**
     * After {@link HandlerMethod} process.
     * 
     * @param result
     *            HandlerMethod returned value
     * @param request
     *            request
     * @param response
     *            response
     * @throws Throwable
     */
    default void afterProcess(Object result, HttpServletRequest request, HttpServletResponse response)
            throws Throwable {

    }

}
