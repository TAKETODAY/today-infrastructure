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
package cn.taketoday.core.bytecode.core;

import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.GeneratorAdapter;

/**
 * Customizes key types for {@link KeyFactory} when building equals, hashCode,
 * and toString. For customization of field types, use
 * {@link FieldTypeCustomizer}
 *
 * @author TODAY <br>
 * 2019-09-03 13:03
 * @see KeyFactory#CLASS_BY_NAME
 */
@FunctionalInterface
public interface Customizer extends KeyFactoryCustomizer {

  void customize(GeneratorAdapter e, Type type);
}
