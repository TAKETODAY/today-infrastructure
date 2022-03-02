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

import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.bind.MissingRequestParameterException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * 数组参数解析器
 * <P>
 * 支持多个参数，也支持一个参数分割成数组
 * </p>
 *
 * @author TODAY <br>
 * 2019-07-07 23:24
 */
public class SimpleArrayParameterResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(final ResolvableMethodParameter parameter) {
    return parameter.isArray(); // TODO
  }

  @Override
  public Object resolveParameter(final RequestContext context, final ResolvableMethodParameter resolvable) throws Throwable {
    final String name = resolvable.getName();
    // parameter value[]
    String[] values = context.getParameters(name);

    if (ObjectUtils.isEmpty(values)) {
      values = StringUtils.split(context.getParameter(name));
      if (ObjectUtils.isEmpty(values)) {
        if (resolvable.isRequired()) {
          throw new MissingRequestParameterException(name, resolvable.getParameterType().getName());
        }
        return null;
      }
    }
    return ObjectUtils.toArrayObject(values, resolvable.getParameterType());
  }

}
