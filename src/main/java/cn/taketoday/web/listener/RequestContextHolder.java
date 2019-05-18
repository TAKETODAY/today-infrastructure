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
package cn.taketoday.web.listener;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

/**
 * @author TODAY <br>
 *         2019-03-23 10:29
 */
public class RequestContextHolder implements ServletRequestListener {

    private static final ThreadLocal<HttpServletRequest> CURRENT_SERVLET_REQUEST = new ThreadLocal<>();

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        resetContext();
    }

    public static void resetContext() {
        CURRENT_SERVLET_REQUEST.remove();
    }

    /**
     * @return
     */
    public static final HttpServletRequest currentRequest() {
        return CURRENT_SERVLET_REQUEST.get();
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        CURRENT_SERVLET_REQUEST.set((HttpServletRequest) sre.getServletRequest());
    }

}
