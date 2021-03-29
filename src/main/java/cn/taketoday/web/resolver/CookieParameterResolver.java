/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.resolver;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.CookieValue;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY <br>
 * 2019-07-12 23:39
 */
public class CookieParameterResolver
        extends AbstractParameterResolver implements ParameterResolver {

  @Override
  public boolean supports(final MethodParameter parameter) {
    return parameter.is(HttpCookie.class);
  }

  @Override
  protected Object missingParameter(MethodParameter parameter) {
    // no cookie
    throw new MissingCookieException(parameter);
  }

  @Override
  protected Object resolveInternal(final RequestContext context, final MethodParameter parameter) {
    final String name = parameter.getName();
    for (final HttpCookie cookie : context.cookies()) {
      if (name.equals(cookie.getName())) {
        return cookie;
      }
    }
    return null;
  }

  public static class CookieAnnotationParameterResolver extends ConvertibleParameterResolver {

    @Override
    public boolean supports(MethodParameter parameter) {
      return parameter.isAnnotationPresent(CookieValue.class);
    }

    @Override
    protected Object missingParameter(MethodParameter parameter) {
      throw new MissingCookieException(parameter);
    }

    @Override
    protected Object resolveInternal(RequestContext context, MethodParameter parameter) {
      final HttpCookie cookie = context.cookie(parameter.getName());
      if (cookie != null) {
        return cookie.getValue();
      }
      return null;
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

  public static class CookieCollectionParameterResolver
          extends CollectionParameterResolver implements ParameterResolver {

    @Override
    protected boolean supportsInternal(MethodParameter parameter) {
      return parameter.isGenericPresent(HttpCookie.class, 0);
    }

    @Override
    protected List<?> resolveCollection(RequestContext context, MethodParameter parameter) throws Throwable {

      final HttpCookie[] cookies = context.cookies();
      final List<HttpCookie> ret = new ArrayList<>(cookies.length);
      Collections.addAll(ret, cookies);
      return ret;
    }
  }
}
