/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.loader;

import java.lang.reflect.Field;

import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.lang.Nullable;

/**
 * @author TODAY 2021/10/3 22:01
 * @since 4.0
 */
public abstract class AbstractPropertyValueResolver implements PropertyValueResolver {

  /**
   * Whether the given field is supported by this resolver.
   */
  protected abstract boolean supportsProperty(PropertyResolvingContext context, Field field);

  @Nullable
  @Override
  public final PropertySetter resolveProperty(PropertyResolvingContext context, Field field) {
    if (supportsProperty(context, field)) {
      return resolveInternal(context, field);
    }
    return null;
  }

  protected abstract PropertySetter resolveInternal(PropertyResolvingContext context, Field field);
}
