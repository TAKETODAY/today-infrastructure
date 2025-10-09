/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.bind.resolver;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.core.MethodParameter;
import infra.http.HttpCookie;
import infra.web.RequestContext;
import infra.web.annotation.CookieValue;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-12 23:39
 */
public class CookieParameterResolver
        extends AbstractNamedValueResolvingStrategy implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return resolvable.is(HttpCookie.class);
  }

  @Override
  protected void handleMissingValue(String name, MethodParameter parameter) {
    // no cookie
    throw new MissingRequestCookieException(name, parameter);
  }

  @Nullable
  @Override
  protected Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext context) throws Exception {
    return context.getCookie(name);
  }

  public static void register(ParameterResolvingStrategies resolvers, ConfigurableBeanFactory beanFactory) {
    resolvers.add(new CookieParameterResolver(),
            new AllCookieParameterResolver(),
            new CookieValueAnnotationParameterResolver(beanFactory),
            new CookieCollectionParameterResolver(beanFactory));
  }

  static class CookieValueAnnotationParameterResolver extends AbstractNamedValueResolvingStrategy {

    public CookieValueAnnotationParameterResolver(@Nullable ConfigurableBeanFactory beanFactory) {
      super(beanFactory);
    }

    @Override
    public boolean supportsParameter(ResolvableMethodParameter resolvable) {
      return resolvable.hasParameterAnnotation(CookieValue.class);
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
        if (resolvable.is(HttpCookie.class)) {
          return cookie;
        }
        return cookie.getValue();
      }
      return null;
    }
  }

  static final class AllCookieParameterResolver implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter resolvable) {
      Class<?> parameterType = resolvable.getParameterType();
      return parameterType.isArray()
              && parameterType.getComponentType() == HttpCookie.class;
    }

    @Override
    public Object resolveArgument(RequestContext requestContext, ResolvableMethodParameter resolvable) {
      return requestContext.getCookies();
    }
  }

  static class CookieCollectionParameterResolver extends AbstractNamedValueResolvingStrategy {

    public CookieCollectionParameterResolver(ConfigurableBeanFactory beanFactory) {
      super(beanFactory);
    }

    @Override
    public boolean supportsParameter(ResolvableMethodParameter resolvable) {
      return resolvable.isCollection()
              && resolvable.getResolvableType().getGeneric(0).resolve() == HttpCookie.class;
    }

    @Nullable
    @Override
    protected Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext context) throws Exception {
      return context.getCookies();
    }

  }

}
