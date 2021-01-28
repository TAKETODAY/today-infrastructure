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

package cn.taketoday.context.reflect;

import cn.taketoday.context.utils.Assert;

/**
 * @author TODAY
 * 2020/9/11 17:27
 */
public class GetterSetterPropertyAccessor implements PropertyAccessor {

  private final GetterMethod getterMethod;
  private final SetterMethod setterMethod;

  public GetterSetterPropertyAccessor(GetterMethod getterMethod, SetterMethod setterMethod) {
    Assert.notNull(getterMethod, "getterMethod must not be null");
    Assert.notNull(setterMethod, "setterMethod must not be null");

    this.getterMethod = getterMethod;
    this.setterMethod = setterMethod;
  }

  @Override
  public Object get(final Object obj) {
    return getterMethod.get(obj);
  }

  @Override
  public void set(final Object obj, final Object value) {
    setterMethod.set(obj, value);
  }
}
