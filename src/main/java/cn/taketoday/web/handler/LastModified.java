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

import cn.taketoday.web.RequestContext;

/**
 * Supports last-modified HTTP requests to facilitate content caching. Same
 * contract as for the Servlet API's {@code getLastModified} method.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY <br>
 *         2019-12-08 20:28
 */
public interface LastModified {

    /**
     * Same contract as for HttpServlet's {@code getLastModified} method. Invoked
     * <b>before</b> request processing.
     * <p>
     * The return value will be sent to the HTTP client as Last-Modified header, and
     * compared with If-Modified-Since headers that the client sends back. The
     * content will only get regenerated if there has been a modification.
     * 
     * @param context
     *            current HTTP request and response
     * @return the time the underlying resource was last modified, or -1 meaning
     *         that the content must always be regenerated
     * @see javax.servlet.http.HttpServlet#getLastModified
     */
    long getLastModified(RequestContext context);

}
