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
package cn.taketoday.context.autowire;

import cn.taketoday.beans.dependency.DependencySetter;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.lang.Nullable;

/**
 * Resolve field property
 *
 * @author TODAY <br>
 * 2018-08-04 15:04
 */
@Deprecated
@FunctionalInterface
public interface PropertyValueResolver {

  /**
   * Resolve {@link DependencySetter}.
   *
   * @param context resolving context
   * @param property bean's field not read only
   * @return property value
   */
  @Nullable
  DependencySetter resolveProperty(PropertyResolvingContext context, BeanProperty property);

}
