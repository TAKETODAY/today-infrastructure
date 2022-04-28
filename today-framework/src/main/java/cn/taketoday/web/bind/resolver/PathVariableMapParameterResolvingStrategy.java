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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * Resolves {@link Map} method arguments annotated with an @{@link PathVariable}
 * where the annotation does not specify a path variable name. The created
 * {@link Map} contains all URI template name/value pairs.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/3 17:52
 */
public class PathVariableMapParameterResolvingStrategy implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    PathVariable ann = resolvable.getParameterAnnotation(PathVariable.class);
    return ann != null
            && Map.class.isAssignableFrom(resolvable.getParameterType())
            && !StringUtils.hasText(ann.value());
  }

  /**
   * Return a Map with all URI template variables or an empty map.
   */
  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    Map<String, String> uriVariables = context.getMatchingMetadata().getUriVariables();
    if (CollectionUtils.isNotEmpty(uriVariables)) {
      return new LinkedHashMap<>(uriVariables);
    }
    else {
      return Collections.emptyMap();
    }
  }

}
