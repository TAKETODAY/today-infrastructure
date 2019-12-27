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
package cn.taketoday.web.resolver.method;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.context.Ordered;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.Cookie;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 *         2019-07-12 23:39
 */
public class CookieParameterResolver implements ParameterResolver {

    @Override
    public boolean supports(final MethodParameter parameter) {
        return parameter.is(HttpCookie.class);
    }

    @Override
    public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {

        final String name = parameter.getName();

        for (final HttpCookie cookie : requestContext.cookies()) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        // no cookie
        if (parameter.isRequired()) {
            throw WebUtils.newBadRequest("Cookie", name, null);
        }
        return null;
    }

    public static class CookieAnnotationParameterResolver extends TypeConverterParameterResolver implements Ordered {

        @Override
        public boolean supports(MethodParameter parameter) {
            return parameter.isAnnotationPresent(Cookie.class);
        }

        @Override
        protected Object resolveSource(final RequestContext requestContext, final MethodParameter parameter) {

            final String name = parameter.getName();
            final HttpCookie[] cookies = requestContext.cookies();
            if (cookies != null) {
                for (final HttpCookie cookie : cookies) {
                    if (name.equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
            return null;
        }

        @Override
        public int getOrder() {
            return HIGHEST_PRECEDENCE;
        }
    }

    public static class CookieArrayParameterResolver implements ParameterResolver {

        @Override
        public boolean supports(MethodParameter parameter) {
            return parameter.isArray() && parameter.getParameterClass().getComponentType() == HttpCookie.class;
        }

        @Override
        public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {
            return requestContext.cookies();
        }
    }

    public static class CookieCollectionParameterResolver extends CollectionParameterResolver implements ParameterResolver {

        @Override
        protected boolean supportsInternal(MethodParameter parameter) {
            return parameter.getParameterClass() == HttpCookie.class;
        }

        @Override
        protected List<?> resolveList(RequestContext requestContext, MethodParameter parameter) throws Throwable {

            final HttpCookie[] cookies = requestContext.cookies();
            final List<HttpCookie> ret = new ArrayList<>(cookies.length);
            Collections.addAll(ret, cookies);
            return ret;
        }
    }
}
