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
package cn.taketoday.web.mapping;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.aware.ApplicationContextSupport;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY <br>
 *         2019-12-24 15:02
 */
public abstract class AbstractHandlerRegistry extends ApplicationContextSupport implements HandlerRegistry, Ordered {

    private Object defaultHandler;

    private int order = Ordered.LOWEST_PRECEDENCE; 

    @Override
    public final Object lookup(final RequestContext context) {
        final Object handler = lookupInternal(context);
        if (handler == null) {
            return getDefaultHandler();
        }
        return handler;
    }

    protected abstract Object lookupInternal(RequestContext context);

    /**
     * Set the default handler for this handler mapping. This handler will be
     * returned if no specific mapping was found.
     * <p>
     * Default is {@code null}, indicating no default handler.
     */
    public void setDefaultHandler(Object defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    /**
     * Return the default handler for this handler mapping, or {@code null} if none.
     */
    public Object getDefaultHandler() {
        return this.defaultHandler;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

}
