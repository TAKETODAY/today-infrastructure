/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.beans.support;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Supplier
 *
 * @author TODAY 2021/5/28 22:16
 * @since 3.0.2
 */
@SuppressWarnings("rawtypes")
final class SupplierInstantiator extends BeanInstantiator {
  private final Supplier supplier;

  SupplierInstantiator(Supplier supplier) {
    this.supplier = supplier;
  }

  @Override
  public Object doInstantiate(@Nullable Object @Nullable [] args) {
    return supplier.get();
  }

}
