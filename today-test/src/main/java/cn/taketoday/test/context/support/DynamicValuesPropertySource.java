/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.context.support;

import java.util.Map;
import java.util.function.Supplier;

import cn.taketoday.core.env.EnumerablePropertySource;
import cn.taketoday.util.StringUtils;

/**
 * {@link EnumerablePropertySource} backed by a map with dynamically supplied
 * values.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2.5
 */
class DynamicValuesPropertySource extends EnumerablePropertySource<Map<String, Supplier<Object>>> {

  DynamicValuesPropertySource(String name, Map<String, Supplier<Object>> valueSuppliers) {
    super(name, valueSuppliers);
  }

  @Override
  public Object getProperty(String name) {
    Supplier<Object> valueSupplier = this.source.get(name);
    return (valueSupplier != null ? valueSupplier.get() : null);
  }

  @Override
  public boolean containsProperty(String name) {
    return this.source.containsKey(name);
  }

  @Override
  public String[] getPropertyNames() {
    return StringUtils.toStringArray(this.source.keySet());
  }

}
