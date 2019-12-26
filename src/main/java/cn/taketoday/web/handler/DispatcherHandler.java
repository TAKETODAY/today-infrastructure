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

import static cn.taketoday.context.exception.ConfigurationException.nonNull;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContext.State;
import cn.taketoday.context.aware.ApplicationContextSupport;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.mapping.CompositeHandlerRegistry;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.HandlerRegistry;
import cn.taketoday.web.resolver.ControllerAdviceExceptionResolver;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.utils.WebUtils;
import cn.taketoday.web.view.ResultHandler;

/**
 * @author TODAY <br>
 *         2019-11-16 19:05
 */
public class DispatcherHandler extends ApplicationContextSupport {

    private static final Logger log = LoggerFactory.getLogger(DispatcherHandler.class);

    private String contextPath;
    /** Action mapping registry */
    private HandlerRegistry handlerRegistry;
    /** exception resolver */
    private ExceptionResolver exceptionResolver;

    private final HandlerAdapter[] requestHandlers = { //
        new RequestHandlerAdapter(), //
        new ViewControllerHandlerAdapter(), //
        new NotFoundRequestAdapter() //
    };

    public DispatcherHandler() {
        this(new ControllerAdviceExceptionResolver());
    }

    public DispatcherHandler(ExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void initApplicationContext(ApplicationContext context) throws ContextException {
        super.initApplicationContext(context);
        if (context instanceof WebApplicationContext) {
            this.contextPath = ((WebApplicationContext) context).getContextPath();
        }
        else {
            throw new ConfigurationException("context must be a WebApplicationContext");
        }
        setMappingRegistry(new CompositeHandlerRegistry(context.getBeans(HandlerRegistry.class)));
    }

    public Object lookupHandler(final RequestContext context) {
        return getHandlerRegistry().lookup(context);
    }

    public HandlerAdapter lookupHandlerAdapter(final Object handler) {
        for (final HandlerAdapter requestHandler : requestHandlers) {
            if (requestHandler.supports(handler)) {
                return requestHandler;
            }
        }
        throw new ConfigurationException("No RequestHandlerAdapter for handler: [" + handler + ']');
    }

    public ResultHandler lookupResultHandler(final Object handler) throws Throwable {
        for (final ResultHandler resultHandler : HandlerMethod.getResultHandlers()) {
            if (resultHandler.supports(handler)) {
                return resultHandler;
            }
        }
        throw new ConfigurationException("No ResultHandler for handler: [" + handler + ']');
    }

    public boolean notModified(final Object handler,
                               final RequestContext context,
                               final HandlerAdapter adapter) throws IOException {

        final String method = context.method();
        // Process last-modified header, if supported by the handler.
        final boolean isGet = "GET".equals(method);
        if (isGet || "HEAD".equals(method)) {
            final long lastModified = adapter.getLastModified(context, handler);
            return isGet && WebUtils.checkNotModified(null, lastModified, context);
        }
        return false;
    }

    public void handle(final RequestContext context) throws Throwable {
        final Object handler = lookupHandler(context);
        handle(handler, context, lookupHandlerAdapter(handler));
    }

    public void handle(final Object handler, final RequestContext context) throws Throwable {
        handle(handler, context, lookupHandlerAdapter(handler));
    }

    public void handle(final Object handler,
                       final RequestContext context,
                       final HandlerAdapter adapter) throws Throwable {

        if (notModified(handler, context, adapter)) {
            return;
        }
        try {
            final Object result = adapter.handle(context, handler);
            if (result != null) {
                lookupResultHandler(handler).handleResult(context, result);
            }
        }
        catch (Throwable e) {
            resolveException(handler, e, context);
        }
    }

    public void resolveException(final Object handler,
                                 final Throwable exception,
                                 final RequestContext context) throws Throwable {

        obtainExceptionResolver().resolveException(context, ExceptionUtils.unwrapThrowable(exception), handler);
    }

    public void destroy() {

        final WebApplicationContext context = obtainApplicationContext();
        if (context != null) {
            final State state = context.getState();
            if (state != State.CLOSING && state != State.CLOSED) {
                context.close();

                final DateFormat dateFormat = new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT);
                final String msg = new StringBuilder("Your application destroyed at: [")
                        .append(dateFormat.format(System.currentTimeMillis()))
                        .append("] on startup date: [")
                        .append(dateFormat.format(context.getStartupDate()))
                        .append(']')
                        .toString();

                log(msg);
            }
        }
    }

    protected void log(final String msg) {
        log.info(msg);
    }

    public ExceptionResolver obtainExceptionResolver() {
        return nonNull(getExceptionResolver(), "You must provide an 'exceptionResolver'");
    }

    public ExceptionResolver getExceptionResolver() {
        return exceptionResolver;
    }

    @Override
    public WebApplicationContext obtainApplicationContext() {
        return (WebApplicationContext) super.obtainApplicationContext();
    }

    public HandlerRegistry getHandlerRegistry() {
        return handlerRegistry;
    }

    public HandlerRegistry obtainHandlerRegistry() {
        return nonNull(getHandlerRegistry(), "You must provide an 'handler registry'");
    }

    public void setMappingRegistry(HandlerRegistry mappingRegistry) {
        this.handlerRegistry = mappingRegistry;
    }

    public String getContextPath() {
        return contextPath;
    }
}
