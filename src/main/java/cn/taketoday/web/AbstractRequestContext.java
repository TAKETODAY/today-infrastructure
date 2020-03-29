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

import java.net.HttpCookie;

import cn.taketoday.web.ui.ModelAndView;

/**
 * @author TODAY <br>
 *         2020-03-29 22:20
 */
public abstract class AbstractRequestContext implements RequestContext {

    private Object requestBody;
    private HttpCookie[] cookies;
    private String[] pathVariables;
    private ModelAndView modelAndView;
    protected static final HttpCookie[] EMPTY_COOKIES = {};

    @Override
    public ModelAndView modelAndView() {
        final ModelAndView ret = this.modelAndView;
        return ret == null ? this.modelAndView = new ModelAndView(this) : ret;
    }

    @Override
    public Object requestBody() {
        return requestBody;
    }

    @Override
    public Object requestBody(Object body) {
        return this.requestBody = body;
    }

    @Override
    public String[] pathVariables() {
        return pathVariables;
    }

    @Override
    public String[] pathVariables(String[] variables) {
        return this.pathVariables = variables;
    }

    // -----------------------------------

    @Override
    public HttpCookie[] cookies() {
        HttpCookie[] cookies = this.cookies;
        if (cookies == null) {
            return this.cookies = getCookiesInternal();
        }
        return cookies;
    }

    @Override
    public HttpCookie cookie(final String name) {

        final HttpCookie[] cookies = cookies();
        if (cookies != null) {
            for (final HttpCookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    /**
     * @return an array of all the Cookies included with this request,or null if the
     *         request has no cookies
     */
    protected abstract HttpCookie[] getCookiesInternal();
}
