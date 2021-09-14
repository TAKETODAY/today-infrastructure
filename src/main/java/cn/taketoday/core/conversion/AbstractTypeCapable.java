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

package cn.taketoday.core.conversion;

import cn.taketoday.core.Assert;

/**
 * target type capable
 *
 * @author TODAY 2021/2/11 12:16
 * @since 3.0
 */
public abstract class AbstractTypeCapable implements TypeCapable {
  protected final Class<?> targetType;

  protected AbstractTypeCapable(Class<?> type) {
    Assert.notNull(type, "type must not be null");
    this.targetType = type;
  }

  @Override
  public Class<?> getTargetType() {
    return targetType;
  }

}
