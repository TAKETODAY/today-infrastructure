/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
