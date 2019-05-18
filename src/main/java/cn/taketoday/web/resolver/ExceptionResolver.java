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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.web.mapping.HandlerMapping;

/**
 * 
 * @author Today <br>
 *         2018-10-24 19:18
 */
@FunctionalInterface
public interface ExceptionResolver {

    /**
     * Resolve exception
     * 
     * @param request
     *            current request
     * @param response
     *            current response
     * @param exception
     *            the exception occurred
     * @param handlerMapping
     *            current handler mapping info
     * @throws Throwable
     */
    void resolveException(HttpServletRequest request, //
            HttpServletResponse response, Throwable exception, HandlerMapping handlerMapping) throws Throwable;

}
