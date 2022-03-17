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

package cn.taketoday.test.context;

import java.util.function.Supplier;

/**
 * Registry used with {@link DynamicPropertySource @DynamicPropertySource}
 * methods so that they can add properties to the {@code Environment} that have
 * dynamically resolved values.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @see DynamicPropertySource
 * @since 5.2.5
 */
public interface DynamicPropertyRegistry {

  /**
   * Add a {@link Supplier} for the given property name to this registry.
   *
   * @param name the name of the property for which the supplier should be added
   * @param valueSupplier a supplier that will provide the property value on demand
   */
  void add(String name, Supplier<Object> valueSupplier);

}
