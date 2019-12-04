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

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.ApplicationContext.State;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mapping.HandlerMapping;
import cn.taketoday.web.mapping.HandlerMappingRegistry;
import cn.taketoday.web.mapping.RegexMapping;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 *         2018-06-25 19:47:14
 * @version 2.3.7
 */
public class DispatcherServlet implements Servlet, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(DispatcherServlet.class);

    /** exception resolver */
    private final ExceptionResolver exceptionResolver;
    /** Action mapping registry */
    private final HandlerMappingRegistry handlerMappingRegistry;

    private final WebServletApplicationContext applicationContext;

    private ServletConfig servletConfig;

    @Autowired
    public DispatcherServlet(ExceptionResolver exceptionResolver, //@off
                             HandlerMappingRegistry handlerMappingRegistry,
                             WebServletApplicationContext applicationContext) //@on
    {
        if (exceptionResolver == null) {
            throw new ConfigurationException("You must provide an 'exceptionResolver'");
        }
        this.exceptionResolver = exceptionResolver;
        this.applicationContext = applicationContext;
        this.handlerMappingRegistry = handlerMappingRegistry;
    }

    /**
     * Prepare {@link RequestContext}
     * 
     * @param r
     *            {@link HttpServletRequest}
     * @param s
     *            {@link HttpServletResponse}
     * @return {@link RequestContext}
     */
    public static RequestContext prepareContext(final HttpServletRequest r, final HttpServletResponse s) {
        return RequestContextHolder.prepareContext(new ServletRequestContext(r, s));
    }

    @Override
    public void service(final ServletRequest req, final ServletResponse res) throws ServletException, IOException {
        service((HttpServletRequest) req, (HttpServletResponse) res);
    }

    public final void service(final HttpServletRequest req,
                              final HttpServletResponse res) throws ServletException, IOException {

        // Lookup handler mapping
        final HandlerMapping mapping = lookupHandlerMapping(req);

        if (mapping == null) {
            res.sendError(404);
            return;
        }

        final RequestContext context = prepareContext(req, res);
        try {
            DispatcherHandler.service(mapping, context);
        }
        catch (Throwable e) {
            try {
                WebUtils.resolveException(context, exceptionResolver, mapping, e);
            }
            catch (Throwable e1) {
                throw new ServletException(e1);
            }
        }
    }

    /**
     * Looking for {@link HandlerMapping}
     * 
     * @param req
     *            Current request
     * @return mapped {@link HandlerMapping}
     * @since 2.3.7
     */
    protected HandlerMapping lookupHandlerMapping(final HttpServletRequest req) { //TODO Optimize performance
        // The key of handler
        String key = req.getMethod().concat(req.getRequestURI());

        final HandlerMappingRegistry registry = this.handlerMappingRegistry;
        final HandlerMapping ret = registry.get(key); // handler mapping
        if (ret == null) {
            // path variable
            key = StringUtils.decodeUrl(key);// decode
            for (final RegexMapping regex : registry.getRegexMappings()) {
                // TODO path matcher pathMatcher.match(requestURI, requestURI)
                if (regex.pattern.matcher(key).matches()) {
                    return regex.handlerMapping;
                }
            }
            log.debug("NOT FOUND -> [{}]", key);
            return null;
        }
        return ret;
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        this.servletConfig = servletConfig;
    }

    @Override
    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    public final String getServletName() {
        return "DispatcherServlet";
    }

    @Override
    public final String getServletInfo() {
        return "DispatcherServlet, Copyright © TODAY & 2017 - 2019 All Rights Reserved";
    }

    @Override
    public void destroy() {

        if (applicationContext != null) {
            final State state = applicationContext.getState();

            if (state != State.CLOSING && state != State.CLOSED) {

                applicationContext.close();

                final DateFormat dateFormat = new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT);//
                final String msg = new StringBuilder()//
                        .append("Your application destroyed at: [")//
                        .append(dateFormat.format(new Date()))//
                        .append("] on startup date: [")//
                        .append(dateFormat.format(applicationContext.getStartupDate()))//
                        .append("]")//
                        .toString();

                log.info(msg);
                servletConfig.getServletContext().log(msg);
            }
        }
    }

    public final HandlerMappingRegistry getHandlerMappingRegistry() {
        return this.handlerMappingRegistry;
    }

    public final ExceptionResolver getExceptionResolver() {
        return this.exceptionResolver;
    }

}
