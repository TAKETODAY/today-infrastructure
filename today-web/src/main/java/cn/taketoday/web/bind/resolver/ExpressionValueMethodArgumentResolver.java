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

import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.handler.method.NamedValueInfo;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * Resolves method arguments annotated with {@code @Value}.
 *
 * <p>An {@code @Value} does not have a name but gets resolved from the default
 * value string, which may contain ${...} placeholder or Spring Expression
 * Language #{...} expressions.
 *
 * <p>A {@link WebDataBinder} may be invoked to apply type conversion to
 * resolved argument value.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/27 13:37
 */
public class ExpressionValueMethodArgumentResolver extends AbstractNamedValueResolvingStrategy {

  /**
   * Create a new {@link ExpressionValueMethodArgumentResolver} instance.
   *
   * @param beanFactory a bean factory to use for resolving  ${...}
   * placeholder and #{...} SpEL expressions in default values;
   * or {@code null} if default values are not expected to contain expressions
   */
  public ExpressionValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
    super(beanFactory);
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return resolvable.hasParameterAnnotation(Value.class);
  }

  @Override
  protected NamedValueInfo getNamedValueInfo(ResolvableMethodParameter resolvable) {
    if (resolvable.hasNamedValueInfo()) {
      return resolvable.getNamedValueInfo();
    }
    Value annotation = resolvable.getParameterAnnotation(Value.class);
    NamedValueInfo namedValueInfo = resolvable.getNamedValueInfo();
    resolvable.withNamedValueInfo(new NamedValueInfo(namedValueInfo, annotation.value()));
    return resolvable.getNamedValueInfo();
  }

  @Nullable
  @Override
  protected Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext context) throws Exception {
    // No name to resolve
    return null;
  }

  @Override
  protected void handleMissingValue(String name, MethodParameter parameter) {
    throw new UnsupportedOperationException("@Value is never required: " + parameter.getMethod());
  }

}
