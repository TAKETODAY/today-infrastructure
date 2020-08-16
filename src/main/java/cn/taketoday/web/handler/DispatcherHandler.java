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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.handler;

import static cn.taketoday.context.exception.ConfigurationException.nonNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import cn.taketoday.context.ApplicationContext.State;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.registry.HandlerRegistry;
import cn.taketoday.web.utils.WebUtils;
import cn.taketoday.web.view.ResultHandler;
import cn.taketoday.web.view.ResultHandlerCapable;
import cn.taketoday.web.view.RuntimeResultHandler;

/**
 * Central dispatcher for HTTP request handlers/controllers
 *
 * @author TODAY <br>
 *         2019-11-16 19:05
 */
public class DispatcherHandler extends WebApplicationContextSupport {

    /** Action mapping registry */
    private HandlerRegistry handlerRegistry;
    private HandlerAdapter[] handlerAdapters;
    private RuntimeResultHandler[] resultHandlers;
    /** exception handler */
    private HandlerExceptionHandler exceptionHandler;

    public DispatcherHandler() {}

    public DispatcherHandler(WebApplicationContext context) {
        setApplicationContext(context);
    }

    // Handler
    // ----------------------------------

    /**
     * Find a suitable handler to handle this HTTP request
     *
     * @param context
     *            Current HTTP request context
     * @return Target handler, if returns {@code null} indicates that there isn't a
     *         handler to handle this request
     */
    public Object lookupHandler(final RequestContext context) {
        return handlerRegistry.lookup(context);
    }

    /**
     * Find a {@link HandlerAdapter} for input handler
     *
     * @param handler
     *            Target handler
     * @return A {@link HandlerAdapter}
     * @throws IllegalStateException
     *             If there isn't a {@link HandlerAdapter} for target handler
     */
    public HandlerAdapter lookupHandlerAdapter(final Object handler) throws IllegalStateException {
        if (handler instanceof HandlerAdapter) {
            return (HandlerAdapter) handler;
        }
        if (handler instanceof HandlerAdapterCapable) {
            return ((HandlerAdapterCapable) handler).getHandlerAdapter();
        }
        for (final HandlerAdapter requestHandler : handlerAdapters) {
            if (requestHandler.supports(handler)) {
                return requestHandler;
            }
        }
        throw new IllegalStateException("No HandlerAdapter for handler: [" + handler + ']');
    }

    /**
     * Find {@link ResultHandler} for handler and handler execution result
     *
     * @param handler
     *            Target handler
     * @param result
     *            Handler execution result
     * @return {@link ResultHandler}
     * @throws IllegalStateException
     *             If there isn't a {@link ResultHandler} for target handler and
     *             handler execution result
     */
    public ResultHandler lookupResultHandler(final Object handler, final Object result) throws IllegalStateException {
        if (handler instanceof ResultHandler) {
            return (ResultHandler) handler;
        }
        if (handler instanceof ResultHandlerCapable) {
            return ((ResultHandlerCapable) handler).getResultHandler();
        }
        for (final RuntimeResultHandler resultHandler : resultHandlers) {
            if (resultHandler.supportsResult(result) || resultHandler.supportsHandler(handler)) {
                return resultHandler;
            }
        }
        throw new IllegalStateException("No RuntimeResultHandler for result: [" + result + ']');
    }

    /**
     * Check if this request is not modified
     *
     * @param handler
     *            Target handler
     * @param context
     *            Current HTTP request context
     * @param adapter
     *            Handler's {@link HandlerAdapter Adapter}
     * @return If not modified
     */
    public boolean notModified(final Object handler,
                               final RequestContext context,
                               final HandlerAdapter adapter) {
        final String method = context.method();
        // Process last-modified header, if supported by the handler.
        final boolean isGet = "GET".equals(method);
        if (isGet || "HEAD".equals(method)) {
            final long lastModified = adapter.getLastModified(context, handler);
            return isGet && WebUtils.checkNotModified(null, lastModified, context);
        }
        return false;
    }

    /**
     * Handle HTTP request
     *
     * @param context
     *            Current HTTP request context
     * @throws Throwable
     */
    public void handle(final RequestContext context) throws Throwable {
        handle(lookupHandler(context), context);
    }

    /**
     * Handle HTTP request
     *
     * @param handler
     * @param context
     *            Current HTTP request context
     * @throws Throwable
     */
    public void handle(final Object handler, final RequestContext context) throws Throwable {
        handle(handler, context, lookupHandlerAdapter(handler));
    }

    /**
     * Handle HTTP request not modify
     *
     * @param handler
     *            Target handler
     * @param context
     *            Current HTTP request context
     * @param adapter
     *            {@link HandlerAdapter}
     * @throws Throwable
     */
    public void handleNotModify(final Object handler,
                                final RequestContext context,
                                final HandlerAdapter adapter) throws Throwable {
        if (!notModified(handler, context, adapter)) {
            handle(handler, context, adapter);
        }
    }

    /**
     * Handle HTTP request
     *
     * @param handler
     *            Target handler
     * @param context
     *            Current HTTP request context
     * @param adapter
     *            {@link HandlerAdapter}
     * @throws Throwable
     */
    public void handle(final Object handler,
                       final RequestContext context,
                       final HandlerAdapter adapter) throws Throwable {
        try {
            final Object result = adapter.handle(context, handler);
            if (result != HandlerAdapter.NONE_RETURN_VALUE) {
                lookupResultHandler(handler, result).handleResult(context, result);
            }
        }
        catch (Throwable e) {
            handleException(handler, e, context);
        }
    }

    /**
     * Handle {@link Exception} occurred in target handler
     *
     * @param handler
     *            Target handler
     * @param exception
     *            {@link Throwable} occurred in target handler
     * @param context
     *            Current HTTP request context
     * @throws Throwable
     *             If {@link Throwable} occurred in {@link HandlerExceptionHandler}
     */
    public void handleException(final Object handler,
                                final Throwable exception,
                                final RequestContext context) throws Throwable {

        obtainExceptionHandler().handleException(context, ExceptionUtils.unwrapThrowable(exception), handler);
    }

    /**
     * Destroy Application
     */
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

    /**
     * Log internal
     *
     * @param msg
     *            Log message
     */
    protected void log(final String msg) {
        log.info(msg);
    }

    public final HandlerAdapter[] getHandlerAdapters() {
        return handlerAdapters;
    }

    public final RuntimeResultHandler[] getResultHandlers() {
        return resultHandlers;
    }

    public final HandlerRegistry getHandlerRegistry() {
        return handlerRegistry;
    }

    public HandlerExceptionHandler obtainExceptionHandler() {
        return nonNull(getExceptionHandler(), "You must provide an 'ExceptionHandler'");
    }

    public HandlerExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public void setHandlerRegistry(HandlerRegistry handlerRegistry) {
        this.handlerRegistry = nonNull(handlerRegistry, "handler registry must not be null");
    }

    public void setHandlerAdapters(HandlerAdapter... handlerAdapters) {
        this.handlerAdapters = nonNull(handlerAdapters, "request handlers must not be null");
    }

    public void setExceptionHandler(HandlerExceptionHandler exceptionHandler) {
        this.exceptionHandler = nonNull(exceptionHandler, "ExceptionHandler must not be null");
    }

    public void setResultHandlers(RuntimeResultHandler... resultHandlers) {
        this.resultHandlers = nonNull(resultHandlers, "result handlers must not be null");
    }
}
