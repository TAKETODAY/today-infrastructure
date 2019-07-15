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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.Session;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 *         2019-07-12 18:04
 */
public class ServletParameterResolver {

    @Singleton
    public static class ServletRequestParameterResolver implements ParameterResolver {

        private final Class<?> servletRequest = ClassUtils.loadClass("javax.servlet.ServletRequest");

        @Override
        public boolean supports(final MethodParameter parameter) {
            return parameter.isInterface() && parameter.isAssignableFrom(ServletRequest.class);
        }

        @Override
        public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {
            return requestContext.nativeRequest();
        }
    }

    @Singleton
    public static class ServletResponseParameterResolver implements ParameterResolver {

        private final Class<?> servletResponse = ClassUtils.loadClass("javax.servlet.ServletResponse");

        @Override
        public boolean supports(final MethodParameter parameter) {
            return parameter.isInterface() && parameter.isAssignableFrom(ServletResponse.class);
        }

        @Override
        public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {
            return requestContext.nativeResponse();
        }
    }

    @Singleton
    public static class SessionParameterResolver implements ParameterResolver {

        private final Class<?> httpSession = ClassUtils.loadClass("javax.servlet.http.HttpSession");

        @Override
        public boolean supports(final MethodParameter parameter) {
            return parameter.isInterface() && parameter.isAssignableFrom(HttpSession.class);
        }

        @Override
        public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {
            return requestContext.nativeSession();
        }
    }

    @Singleton
    public static class ServletContextParameterResolver implements ParameterResolver {

        private final Class<?> servletContext = ClassUtils.loadClass("javax.servlet.ServletContext");

        @Override
        public boolean supports(final MethodParameter parameter) {
            return parameter.getParameterClass() == ServletContext.class;
        }

        @Override
        public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {
            return requestContext.nativeSession();
        }
    }

    // ------------- cookie

    @Singleton
    public static class ServletCookieParameterResolver implements ParameterResolver {

        @Override
        public boolean supports(final MethodParameter parameter) {
            return parameter.getParameterClass() == Cookie.class;
        }

        @Override
        public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {

            final String name = parameter.getName();
            for (final Cookie cookie : requestContext.nativeRequest(HttpServletRequest.class).getCookies()) {
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
    }

    @Singleton
    public static class ServletCookieCollectionParameterResolver extends CollectionParameterResolver implements ParameterResolver {

        @Override
        protected boolean supportsInternal(MethodParameter parameter) {
            return parameter.getParameterClass() == Cookie.class;
        }

        @Override
        protected List<?> resolveList(RequestContext requestContext, MethodParameter parameter) throws Throwable {

            final Cookie[] cookies = requestContext.nativeRequest(HttpServletRequest.class).getCookies();
            final List<Cookie> ret = new ArrayList<>(cookies.length);
            Collections.addAll(ret, cookies);
            return ret;
        }
    }

    @Singleton
    public static class ServletCookieArrayParameterResolver implements ParameterResolver {

        @Override
        public boolean supports(MethodParameter parameter) {
            return parameter.isArray() && parameter.getParameterClass().getComponentType() == Cookie.class;
        }

        @Override
        public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {
            return requestContext.nativeRequest(HttpServletRequest.class).getCookies();
        }
    }

//----------- session
    @Singleton
    public static class ServletSessionAttributeParameterResolver implements ParameterResolver {

        @Override
        public boolean supports(MethodParameter parameter) {
            return parameter.isAnnotationPresent(Session.class);
        }

        @Override
        public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {
            return requestContext.nativeSession(HttpSession.class).getAttribute(parameter.getName());
        }
    }
}
