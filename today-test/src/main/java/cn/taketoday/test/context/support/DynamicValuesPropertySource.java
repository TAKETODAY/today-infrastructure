/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.test.context.support;

import java.util.Map;
import java.util.function.Supplier;

import cn.taketoday.core.env.EnumerablePropertySource;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.util.function.SupplierUtils;

/**
 * {@link EnumerablePropertySource} backed by a map with dynamically supplied
 * values.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DynamicValuesPropertySource extends MapPropertySource {

  @SuppressWarnings({ "rawtypes", "unchecked" })
  DynamicValuesPropertySource(String name, Map<String, Supplier<Object>> valueSuppliers) {
    super(name, (Map) valueSuppliers);
  }

  @Override
  public Object getProperty(String name) {
    return SupplierUtils.resolve(super.getProperty(name));
  }

}
