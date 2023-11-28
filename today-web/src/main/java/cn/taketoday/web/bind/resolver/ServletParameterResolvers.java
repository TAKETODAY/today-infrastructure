/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.bind.resolver;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.CookieValue;
import cn.taketoday.web.annotation.ServletContextAttribute;
import cn.taketoday.web.annotation.SessionAttribute;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.servlet.ServletUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-12 18:04
 */
public class ServletParameterResolvers {

  public static void register(ConfigurableBeanFactory beanFactory,
          ParameterResolvingStrategies resolvers, ServletContext context) {
    resolvers.add(new ServletRequestMethodArgumentResolver());
    // Servlet cookies parameter
    // ----------------------------
    resolvers.add(new ForCookie(beanFactory));
    resolvers.add(new ForCookieArray());
    resolvers.add(new ForCookieCollection(beanFactory));
    // Servlet components parameter
    // ----------------------------
    resolvers.add(new ForHttpSession());
    resolvers.add(new ForServletRequest());
    resolvers.add(new ForServletResponse());
    resolvers.add(new ForServletContext(context));
    // Attributes
    // ------------------------
    resolvers.add(new ForHttpSessionAttribute());
    resolvers.add(new ForServletContextAttribute(beanFactory, context));
    resolvers.add(new PrincipalMethodArgumentResolver());
  }

  static class ForServletRequest implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.isInterface() && parameter.isAssignableTo(ServletRequest.class);
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return ServletUtils.getServletRequest(context);
    }
  }

  static class ForServletResponse implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.isInterface() && parameter.isAssignableTo(ServletResponse.class);
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return ServletUtils.getServletResponse(context);
    }
  }

  static class ForHttpSession implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.isAssignableTo(HttpSession.class);
    }

    @Override
    public Object resolveArgument(
            RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return ServletUtils.getHttpSession(context);
    }
  }

  static class ForHttpSessionAttribute implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.hasParameterAnnotation(SessionAttribute.class);
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      HttpSession httpSession = ServletUtils.getHttpSession(context, false);
      if (httpSession == null) {
        return null;
      }
      return httpSession.getAttribute(resolvable.getName());
    }
  }

  static class ForServletContext implements ParameterResolvingStrategy {

    private final ServletContext servletContext;

    public ForServletContext(ServletContext servletContext) {
      this.servletContext = servletContext;
    }

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.is(ServletContext.class);
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return servletContext;
    }
  }

  // ------------- cookie

  static class ForCookieCollection extends AbstractNamedValueResolvingStrategy {

    public ForCookieCollection(ConfigurableBeanFactory beanFactory) {
      super(beanFactory);
    }

    @Override
    public boolean supportsParameter(ResolvableMethodParameter resolvable) {
      return resolvable.isCollection()
              && resolvable.getResolvableType().getGeneric(0).resolve() == Cookie.class;
    }

    @Nullable
    @Override
    protected Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext context) throws Exception {
      return ServletUtils.getServletRequest(context).getCookies();
    }

  }

  static class ForCookieArray implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.isArray()
              && parameter.getParameterType().getComponentType() == Cookie.class;
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return ServletUtils.getServletRequest(context).getCookies();
    }
  }

  private static class ForCookie extends AbstractNamedValueResolvingStrategy {

    public ForCookie(@Nullable ConfigurableBeanFactory beanFactory) {
      super(beanFactory);
    }

    @Override
    public boolean supportsParameter(ResolvableMethodParameter resolvable) {
      return resolvable.is(Cookie.class)
              || resolvable.is(HttpCookie.class)
              || resolvable.hasParameterAnnotation(CookieValue.class);
    }

    @Override
    protected void handleMissingValue(String name, MethodParameter parameter) {
      throw new MissingRequestCookieException(name, parameter);
    }

    @Override
    protected void handleMissingValueAfterConversion(String name, MethodParameter parameter, RequestContext request) {
      throw new MissingRequestCookieException(name, parameter, true);
    }

    @Nullable
    @Override
    protected Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext context) {
      HttpCookie cookie = context.getCookie(name);
      if (cookie != null) {
        if (resolvable.is(Cookie.class)) {
          return new Cookie(cookie.getName(), cookie.getValue());
        }
        if (resolvable.is(HttpCookie.class)) {
          return cookie;
        }
        return cookie.getValue();
      }
      return null;
    }
  }

  static class ForServletContextAttribute extends AbstractNamedValueResolvingStrategy {
    private final ServletContext servletContext;

    public ForServletContextAttribute(ConfigurableBeanFactory beanFactory, ServletContext servletContext) {
      super(beanFactory);
      this.servletContext = servletContext;
    }

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.hasParameterAnnotation(ServletContextAttribute.class);
    }

    @Nullable
    @Override
    protected Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext context) throws Exception {
      return servletContext.getAttribute(name);
    }

  }
}
