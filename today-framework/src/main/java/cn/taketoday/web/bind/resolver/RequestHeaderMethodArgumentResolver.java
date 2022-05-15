/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.util.List;
import java.util.Map;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestHeader;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * Resolves method arguments annotated with {@code @RequestHeader} except for
 * {@link Map} arguments. See {@link RequestHeaderMapMethodArgumentResolver} for
 * details on {@link Map} arguments annotated with {@code @RequestHeader}.
 *
 * <p>An {@code @RequestHeader} is a named value resolved from a request header.
 * It has a required flag and a default value to fall back on when the request
 * header does not exist.
 *
 * <p>A {@link WebDataBinder} is invoked to apply type conversion to resolved
 * request header values that don't yet match the method parameter type.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-13 11:11
 */
public class RequestHeaderMethodArgumentResolver extends AbstractNamedValueResolvingStrategy {

  /**
   * Create a new {@link RequestHeaderMethodArgumentResolver} instance.
   *
   * @param beanFactory a bean factory to use for resolving  ${...}
   * placeholder and #{...} SpEL expressions in default values;
   * or {@code null} if default values are not expected to have expressions
   */
  public RequestHeaderMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
    super(beanFactory);
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter parameter) {
    return parameter.hasParameterAnnotation(RequestHeader.class)
            && !parameter.isAssignableTo(Map.class);
  }

  @Nullable
  @Override
  protected Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext context) throws Exception {
    final HttpHeaders httpHeaders = context.requestHeaders();
    List<String> headerValues = httpHeaders.get(name);
    if (headerValues != null) {
      return headerValues.size() == 1 ? headerValues.get(0) : headerValues;
    }
    else {
      return null;
    }
  }

  @Override
  protected void handleMissingValue(String name, MethodParameter parameter) {
    throw new MissingRequestHeaderException(name, parameter);
  }

  @Override
  protected void handleMissingValueAfterConversion(String name, MethodParameter parameter, RequestContext request) throws Exception {
    throw new MissingRequestHeaderException(name, parameter, true);
  }

}
