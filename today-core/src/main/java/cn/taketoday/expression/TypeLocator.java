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

package cn.taketoday.expression;

import cn.taketoday.expression.spel.support.StandardTypeLocator;

/**
 * Implementers of this interface are expected to be able to locate types.
 * They may use a custom {@link ClassLoader} and/or deal with common
 * package prefixes (e.g. {@code java.lang}) however they wish.
 *
 * <p>See {@link StandardTypeLocator}
 * for an example implementation.
 *
 * @author Andy Clement
 * @since 4.0
 */
@FunctionalInterface
public interface TypeLocator {

  /**
   * Find a type by name. The name may or may not be fully qualified
   * (e.g. {@code String} or {@code java.lang.String}).
   *
   * @param typeName the type to be located
   * @return the {@code Class} object representing that type
   * @throws EvaluationException if there is a problem finding the type
   */
  Class<?> findType(String typeName) throws EvaluationException;

}
