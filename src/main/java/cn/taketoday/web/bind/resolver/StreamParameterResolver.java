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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import cn.taketoday.core.io.OutputStreamSource;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.view.Model;

/**
 * @author TODAY <br>
 * 2019-07-09 22:49
 */
public class StreamParameterResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(final ResolvableMethodParameter parameter) {

    final Class<?> parameterClass = parameter.getParameterType();
    return parameterClass == Readable.class//
            || parameterClass == OutputStreamSource.class//
            || parameterClass == Reader.class//
            || parameterClass == Writer.class//
            || parameterClass == InputStream.class//
            || parameterClass == OutputStream.class;
  }

  /**
   * Resolve {@link Model} parameter.
   */
  @Override
  public Object resolveParameter(final RequestContext context, final ResolvableMethodParameter resolvable) throws Throwable {

    final Class<?> parameterClass = resolvable.getParameterType();

    if (parameterClass == Readable.class || parameterClass == OutputStreamSource.class) {
      return context;
    }

    if (parameterClass == Reader.class) {
      return context.getReader();
    }
    if (parameterClass == Writer.class) {
      return context.getWriter();
    }

    if (parameterClass == InputStream.class) {
      return context.getInputStream();
    }

    return context.getOutputStream();
  }

}
