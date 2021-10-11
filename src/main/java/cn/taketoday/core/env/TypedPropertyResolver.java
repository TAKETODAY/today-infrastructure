/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.core.env;

import cn.taketoday.lang.Nullable;

/**
 * @author TODAY 2021/10/3 15:33
 * @since 4.0
 */
public abstract class TypedPropertyResolver extends AbstractPropertyResolver {

  @Override
  @Nullable
  public String getProperty(String key) {
    return getProperty(key, String.class, true);
  }

  @Override
  @Nullable
  public <T> T getProperty(String key, Class<T> targetValueType) {
    return getProperty(key, targetValueType, true);
  }

  @Override
  @Nullable
  protected String getPropertyAsRawString(String key) {
    return getProperty(key, String.class, false);
  }

  public abstract <T> T getProperty(
          String key, Class<T> targetValueType, boolean resolveNestedPlaceholders);

}
