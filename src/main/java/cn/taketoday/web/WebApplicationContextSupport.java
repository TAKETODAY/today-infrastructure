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
package cn.taketoday.web;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextSupport;
import cn.taketoday.context.exception.ContextException;

/**
 * @author TODAY <br>
 *         2019-12-27 09:36
 */
public class WebApplicationContextSupport extends ApplicationContextSupport {

    public final String getContextPath() {
        return getWebApplicationContext().getContextPath();
    }

    @Override
    protected void initApplicationContext() throws ContextException {
        super.initApplicationContext();
    }

    /**
     * Return the current application context as {@link WebApplicationContext}.
     * 
     * @throws IllegalStateException
     *             if not running in a WebApplicationContext
     * @see #getApplicationContext()
     */
    public final WebApplicationContext getWebApplicationContext() throws IllegalStateException {
        final ApplicationContext ctx = getApplicationContext();
        if (ctx instanceof WebApplicationContext) {
            return (WebApplicationContext) ctx;
        }
        throw new IllegalStateException("ApplicationContext must be a WebApplicationContext");
    }

}
