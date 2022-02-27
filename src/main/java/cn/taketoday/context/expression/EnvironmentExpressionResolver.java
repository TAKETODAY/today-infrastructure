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

package cn.taketoday.context.expression;

import cn.taketoday.core.env.Environment;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionResolver;
import cn.taketoday.lang.Nullable;

/**
 * Read-only EL property accessor that knows how to retrieve keys
 * of a {@link Environment} instance.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/27 21:36
 */
public class EnvironmentExpressionResolver extends ExpressionResolver {

  @Override
  public Object getValue(ExpressionContext context, @Nullable Object base, Object property) {
    if (base instanceof Environment environment && property instanceof String) {
      context.setPropertyResolved(base, property);
      return environment.getProperty((String) property);
    }
    return null;
  }

  @Override
  public Class<?> getType(ExpressionContext context, Object base, Object property) {
    if (base instanceof Environment environment && property instanceof String) {
      context.setPropertyResolved(base, property);
      return environment.containsProperty((String) property) ? String.class : null;
    }
    return null;
  }

  @Override
  public void setValue(ExpressionContext context, Object base, Object property, Object value) {
    // read-only
  }

  @Override
  public boolean isReadOnly(ExpressionContext context, Object base, Object property) {
    return true;
  }

}
