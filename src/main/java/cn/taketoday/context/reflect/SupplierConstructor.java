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

import java.util.function.Supplier;

import cn.taketoday.context.utils.Assert;

/**
 * Supplier
 *
 * @author TODAY 2021/5/28 22:16
 * @since 3.0.2
 */
public final class SupplierConstructor<T> implements ConstructorAccessor {
  private final Supplier<T> supplier;

  public SupplierConstructor(Supplier<T> supplier) {
    Assert.notNull(supplier, "instance supplier must not be null");
    this.supplier = supplier;
  }

  @Override
  public Object newInstance(Object[] args) {
    return supplier.get();
  }

}
