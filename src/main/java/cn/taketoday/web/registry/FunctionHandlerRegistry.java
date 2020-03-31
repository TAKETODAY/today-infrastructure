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
package cn.taketoday.web.registry;

import static cn.taketoday.web.RequestMethod.DELETE;
import static cn.taketoday.web.RequestMethod.GET;
import static cn.taketoday.web.RequestMethod.HEAD;
import static cn.taketoday.web.RequestMethod.OPTIONS;
import static cn.taketoday.web.RequestMethod.PATCH;
import static cn.taketoday.web.RequestMethod.POST;
import static cn.taketoday.web.RequestMethod.PUT;
import static cn.taketoday.web.RequestMethod.TRACE;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.handler.FunctionHandler;
import cn.taketoday.web.handler.RequestHandler;

/**
 * @author TODAY <br>
 *         2019-12-26 17:33
 */
@MissingBean(type = FunctionHandlerRegistry.class)
public class FunctionHandlerRegistry extends MappedHandlerRegistry {

    public FunctionHandlerRegistry() {
        this(new HashMap<>());
    }

    public FunctionHandlerRegistry(int initialCapacity) {
        this(new HashMap<>(initialCapacity));
    }

    public FunctionHandlerRegistry(Map<String, Object> handlers) {
        super(handlers);
    }

    public FunctionHandlerRegistry(Map<String, Object> handlers, int order) {
        super(handlers, order);
    }

    @Override
    protected String computeKey(final RequestContext context) {
        return context.method().concat(context.requestURI());
    }

    // HEAD
    // ---------------------------------------

    public FunctionHandlerRegistry head(String pathPattern, RequestHandler handler) {
        return HEAD(pathPattern, handler);
    }

    public FunctionHandlerRegistry head(String pathPattern, FunctionHandler handler) {
        return HEAD(pathPattern, handler);
    }

    public FunctionHandlerRegistry HEAD(String pathPattern, Object handler) {
        return register(HEAD, pathPattern, handler);
    }

    // PUT
    // ---------------------------------------

    public FunctionHandlerRegistry put(String pathPattern, RequestHandler handler) {
        return PUT(pathPattern, handler);
    }

    public FunctionHandlerRegistry put(String pathPattern, FunctionHandler handler) {
        return PUT(pathPattern, handler);
    }

    public FunctionHandlerRegistry PUT(String pathPattern, Object handler) {
        return register(PUT, pathPattern, handler);
    }

    // DELETE 
    // ---------------------------------------

    public FunctionHandlerRegistry delete(String pathPattern, RequestHandler handler) {
        return DELETE(pathPattern, handler);
    }

    public FunctionHandlerRegistry delete(String pathPattern, FunctionHandler handler) {
        return DELETE(pathPattern, handler);
    }

    public FunctionHandlerRegistry DELETE(String pathPattern, Object handler) {
        return register(DELETE, pathPattern, handler);
    }
    // PATCH 
    // ---------------------------------------

    public FunctionHandlerRegistry patch(String pathPattern, RequestHandler handler) {
        return PATCH(pathPattern, handler);
    }

    public FunctionHandlerRegistry patch(String pathPattern, FunctionHandler handler) {
        return PATCH(pathPattern, handler);
    }

    public FunctionHandlerRegistry PATCH(String pathPattern, Object handler) {
        return register(PATCH, pathPattern, handler);
    }

    // TRACE 
    // ---------------------------------------

    public FunctionHandlerRegistry trace(String pathPattern, RequestHandler handler) {
        return TRACE(pathPattern, handler);
    }

    public FunctionHandlerRegistry trace(String pathPattern, FunctionHandler handler) {
        return TRACE(pathPattern, handler);
    }

    public FunctionHandlerRegistry TRACE(String pathPattern, Object handler) {
        return register(TRACE, pathPattern, handler);
    }

    // OPTIONS 
    // ---------------------------------------

    public FunctionHandlerRegistry options(String pathPattern, RequestHandler handler) {
        return OPTIONS(pathPattern, handler);
    }

    public FunctionHandlerRegistry options(String pathPattern, FunctionHandler handler) {
        return OPTIONS(pathPattern, handler);
    }

    public FunctionHandlerRegistry OPTIONS(String pathPattern, Object handler) {
        return register(OPTIONS, pathPattern, handler);
    }

    // POST 
    // ---------------------------------------

    public FunctionHandlerRegistry post(String pathPattern, RequestHandler handler) {
        return POST(pathPattern, handler);
    }

    public FunctionHandlerRegistry post(String pathPattern, FunctionHandler handler) {
        return POST(pathPattern, handler);
    }

    public FunctionHandlerRegistry POST(String pathPattern, Object handler) {
        return register(POST, pathPattern, handler);
    }

    // GET 
    // ---------------------------------------

    public FunctionHandlerRegistry get(String pathPattern, RequestHandler handler) {
        return GET(pathPattern, handler);
    }

    public FunctionHandlerRegistry get(String pathPattern, FunctionHandler handler) {
        return GET(pathPattern, handler);
    }

    public FunctionHandlerRegistry GET(String pathPattern, Object handler) {
        return register(GET, pathPattern, handler);
    }

    /**
     * Map a handler to the given URL path (or pattern)
     * 
     * @param method
     *            Target HTTP request method
     * @param pathPattern
     *            Target path pattern
     * @param handler
     *            Handler object
     * @return This {@link FunctionHandlerRegistry}
     */
    public FunctionHandlerRegistry register(RequestMethod method, String pathPattern, Object handler) {
        registerHandler(method.name().concat(pathPattern), handler);
        return this;
    }

}
