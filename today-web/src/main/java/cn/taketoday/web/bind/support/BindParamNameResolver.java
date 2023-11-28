/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.bind.support;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.util.StringUtils;
import cn.taketoday.validation.DataBinder;
import cn.taketoday.web.bind.annotation.BindParam;

/**
 * {@link cn.taketoday.validation.DataBinder.NameResolver} that determines
 * the bind value name from a {@link BindParam @BindParam} method parameter
 * annotation.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class BindParamNameResolver implements DataBinder.NameResolver {

  @Override
  public String resolveName(MethodParameter parameter) {
    BindParam bindParam = parameter.getParameterAnnotation(BindParam.class);
    if (bindParam != null) {
      if (StringUtils.hasText(bindParam.value())) {
        return bindParam.value();
      }
    }
    return null;
  }

}
