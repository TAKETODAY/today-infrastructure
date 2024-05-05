/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.mock.bind.resolver;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.MockResponse;
import cn.taketoday.mock.api.http.Cookie;
import cn.taketoday.mock.api.http.HttpSession;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.CookieValue;
import cn.taketoday.web.annotation.SessionAttribute;
import cn.taketoday.web.bind.resolver.AbstractNamedValueResolvingStrategy;
import cn.taketoday.web.bind.resolver.MissingRequestCookieException;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategies;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.mock.MockUtils;
import cn.taketoday.web.mock.bind.MockContextAttribute;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-12 18:04
 */
public class MockParameterResolvers {

  public static void register(ConfigurableBeanFactory beanFactory,
          ParameterResolvingStrategies resolvers, MockContext context) {
    resolvers.add(new MockRequestMethodArgumentResolver());
    // Servlet cookies parameter
    // ----------------------------
    resolvers.add(new ForCookie(beanFactory));
    resolvers.add(new ForCookieArray());
    resolvers.add(new ForCookieCollection(beanFactory));
    // Servlet components parameter
    // ----------------------------
    resolvers.add(new ForHttpSession());
    resolvers.add(new ForMockRequest());
    resolvers.add(new ForMockResponse());
    resolvers.add(new ForMockContext(context));
    // Attributes
    // ------------------------
    resolvers.add(new ForHttpSessionAttribute());
    resolvers.add(new ForMockContextAttribute(beanFactory, context));
    resolvers.add(new PrincipalMethodArgumentResolver());
  }

  static class ForMockRequest implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.isInterface() && parameter.isAssignableTo(MockRequest.class);
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return MockUtils.getServletRequest(context);
    }
  }

  static class ForMockResponse implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.isInterface() && parameter.isAssignableTo(MockResponse.class);
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return MockUtils.getServletResponse(context);
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
      return MockUtils.getHttpSession(context);
    }
  }

  static class ForHttpSessionAttribute implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.hasParameterAnnotation(SessionAttribute.class);
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      HttpSession httpSession = MockUtils.getHttpSession(context, false);
      if (httpSession == null) {
        return null;
      }
      return httpSession.getAttribute(resolvable.getName());
    }
  }

  static class ForMockContext implements ParameterResolvingStrategy {

    private final MockContext mockContext;

    public ForMockContext(MockContext mockContext) {
      this.mockContext = mockContext;
    }

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.is(MockContext.class);
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return mockContext;
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
      return MockUtils.getServletRequest(context).getCookies();
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
      return MockUtils.getServletRequest(context).getCookies();
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

  static class ForMockContextAttribute extends AbstractNamedValueResolvingStrategy {
    private final MockContext mockContext;

    public ForMockContextAttribute(ConfigurableBeanFactory beanFactory, MockContext mockContext) {
      super(beanFactory);
      this.mockContext = mockContext;
    }

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.hasParameterAnnotation(MockContextAttribute.class);
    }

    @Nullable
    @Override
    protected Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext context) throws Exception {
      return mockContext.getAttribute(name);
    }

  }
}
