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
package cn.taketoday.web.servlet;

import static cn.taketoday.context.exception.ConfigurationException.nonNull;
import static cn.taketoday.web.RequestContextHolder.prepareContext;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.handler.HandlerAdapter;

/**
 * @author TODAY <br>
 *         2018-06-25 19:47:14
 * @version 2.3.7
 */
public class DispatcherServlet extends DispatcherHandler implements Servlet, Serializable {

    private static final long serialVersionUID = 1L;

    private ServletConfig servletConfig;

    public DispatcherServlet() {}

    public DispatcherServlet(WebServletApplicationContext context) {
        setApplicationContext(context);
    }
    
    @Override
    public void service(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {

        final RequestContext context = prepareContext(new ServletRequestContext((HttpServletRequest) request,
                                                                                (HttpServletResponse) response));
        // Lookup handler
        final Object handler = lookupHandler(context);
        final HandlerAdapter adapter = lookupHandlerAdapter(handler);
        try {
            handle(handler, context, adapter);
        }
        catch (final Throwable e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        this.servletConfig = servletConfig;
    }

    @Override
    public ServletConfig getServletConfig() {
        return nonNull(servletConfig, "DispatcherServlet has not been initialized");
    }

    public String getServletName() {
        return "DispatcherServlet";
    }

    @Override
    public final String getServletInfo() {
        return "DispatcherServlet, Copyright © TODAY & 2017 - 2020 All Rights Reserved";
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    protected void log(String msg) {
        super.log(msg);
        getServletConfig().getServletContext().log(msg);
    }

}
