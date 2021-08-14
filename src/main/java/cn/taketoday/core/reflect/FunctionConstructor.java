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

package cn.taketoday.core.reflect;

import java.util.function.Function;

import cn.taketoday.core.utils.Assert;

/**
 * Function
 *
 * @author TODAY 2021/5/28 22:19
 * @since 3.0.2
 */
public final class FunctionConstructor<T> implements ConstructorAccessor {
  private final Function<Object[], T> function;

  public FunctionConstructor(Function<Object[], T> function) {
    Assert.notNull(function, "instance function must not be null");
    this.function = function;
  }

  @Override
  public Object newInstance(Object[] args) {
    return function.apply(args);
  }
}
