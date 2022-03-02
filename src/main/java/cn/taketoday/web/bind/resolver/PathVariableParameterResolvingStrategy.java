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

import java.util.Map;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * Resolves method arguments annotated with an @{@link PathVariable}.
 *
 * <p>An @{@link PathVariable} is a named value that gets resolved from a URI template variable.
 * It is always required and does not have a default value to fall back on. See the base class
 * {@link AbstractNamedValueResolvingStrategy}
 * for more information on how named values are processed.
 *
 * <p>If the method parameter type is {@link Map}, the name specified in the annotation is used
 * to resolve the URI variable String value. The value is then converted to a {@link Map} via
 * type conversion, assuming a suitable {@link Converter} has been registered.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/3 16:32
 */
public class PathVariableParameterResolvingStrategy extends AbstractNamedValueResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    if (resolvable.hasParameterAnnotation(PathVariable.class)) {
      if (Map.class.isAssignableFrom(resolvable.nestedIfOptional().getNestedParameterType())) {
        PathVariable pathVariable = resolvable.getParameterAnnotation(PathVariable.class);
        return pathVariable != null && StringUtils.hasText(pathVariable.value());
      }
      return true;
    }
    return false;
  }

  @Nullable
  @Override
  protected Object resolveName(
          String name, ResolvableMethodParameter resolvable, RequestContext context) throws Exception {
    return context.getMatchingMetadata().getUriVariable(name);
  }

  @Override
  protected void handleMissingValue(String name, MethodParameter parameter) {
    throw new MissingPathVariableException(name, parameter);
  }

  @Override
  protected void handleMissingValueAfterConversion(
          String name, MethodParameter parameter, RequestContext request) {
    throw new MissingPathVariableException(name, parameter, true);
  }

}

