/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 * <p>
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.handler;

import java.util.Objects;

import cn.taketoday.core.PathMatcher;
import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.resolver.MissingPathVariableParameterException;
import cn.taketoday.web.resolver.ParameterConversionException;

/**
 * @author TODAY <br>
 * 2019-12-28 14:56
 */
public class PathVariableMethodParameter extends MethodParameter {

  private final int variableIndex;
  private final String pathPattern;
  private final PathMatcher pathMatcher;

  public PathVariableMethodParameter(int variableIndex,
                                     String pathPattern,
                                     HandlerMethod handler,
                                     MethodParameter other,
                                     PathMatcher pathMatcher) {
    super(handler, other);
    this.variableIndex = variableIndex;
    this.pathPattern = pathPattern;
    this.pathMatcher = pathMatcher;
  }

  @Override
  public Object resolveParameter(final RequestContext request) throws Throwable {
    String[] pathVariables = request.pathVariables();
    if (pathVariables == null) {
      String requestURI = StringUtils.decodeUrl(request.getRequestPath());
      pathVariables = request.pathVariables(pathMatcher.extractVariables(pathPattern, requestURI));
      if (pathVariables == null) {
        throw new MissingPathVariableParameterException(this);
      }
    }
    try {
      return DefaultConversionService.getSharedInstance()
              .convert(pathVariables[variableIndex], getGenericDescriptor());
    }
    catch (ConversionException e) {
      throw new ParameterConversionException(this, pathVariables[variableIndex], e);
    }
  }

  public int getVariableIndex() {
    return variableIndex;
  }

  public String getPathPattern() {
    return pathPattern;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof PathVariableMethodParameter)) return false;
    if (!super.equals(o)) return false;
    final PathVariableMethodParameter that = (PathVariableMethodParameter) o;
    return variableIndex == that.variableIndex
            && Objects.equals(pathPattern, that.pathPattern)
            && Objects.equals(pathMatcher, that.pathMatcher);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), variableIndex, pathPattern, pathMatcher);
  }
}
