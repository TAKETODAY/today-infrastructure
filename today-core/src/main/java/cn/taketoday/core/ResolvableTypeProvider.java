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

package cn.taketoday.core;

import cn.taketoday.lang.Nullable;

/**
 * Any object can implement this interface to provide its actual {@link ResolvableType}.
 *
 * <p>Such information is very useful when figuring out if the instance matches a generic
 * signature as Java does not convey the signature at runtime.
 *
 * <p>Users of this interface should be careful in complex hierarchy scenarios, especially
 * when the generic type signature of the class changes in sub-classes. It is always
 * possible to return {@code null} to fallback on a default behavior.
 *
 * @author Stephane Nicoll
 * @author TODAY 2021/3/23 22:01
 * @since 3.0
 */
public interface ResolvableTypeProvider {

  /**
   * Return the {@link ResolvableType} describing this instance
   * (or {@code null} if some sort of default should be applied instead).
   */
  @Nullable
  ResolvableType getResolvableType();

}
