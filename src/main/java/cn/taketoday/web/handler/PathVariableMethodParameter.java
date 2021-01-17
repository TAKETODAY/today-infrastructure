/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import cn.taketoday.context.PathMatcher;
import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.resolver.ParameterConversionException;
import cn.taketoday.web.utils.WebUtils;

import static cn.taketoday.context.utils.ConvertUtils.convert;

/**
 * @author TODAY <br>
 * 2019-12-28 14:56
 */
public class PathVariableMethodParameter extends MethodParameter {

  private final int pathIndex;
  private final String pathPattern;
  private final PathMatcher pathMatcher;

  public PathVariableMethodParameter(int pathIndex,
                                     String pathPattern,
                                     HandlerMethod handler,
                                     MethodParameter other,
                                     PathMatcher pathMatcher) {
    super(handler, other);
    this.pathIndex = pathIndex;
    this.pathPattern = pathPattern;
    this.pathMatcher = pathMatcher;
  }

  @Override
  protected Object resolveParameter(final RequestContext request) throws Throwable {

    String[] pathVariables = request.pathVariables();
    if (pathVariables == null) {
      String requestURI = StringUtils.decodeUrl(request.requestURI());
      pathVariables = request.pathVariables(pathMatcher.extractVariables(pathPattern, requestURI));
      if (pathVariables == null) {
        throw WebUtils.newBadRequest("Path variable", getName(), null);
      }
    }
    try {
      return convert(pathVariables[pathIndex], getParameterClass());
    }
    catch (ConversionException e) {
      throw new ParameterConversionException(this, pathVariables[pathIndex], e);
    }
  }

  public int getPathIndex() {
    return pathIndex;
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
    return pathIndex == that.pathIndex &&
            Objects.equals(pathPattern, that.pathPattern) &&
            Objects.equals(pathMatcher, that.pathMatcher);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), pathIndex, pathPattern, pathMatcher);
  }
}
