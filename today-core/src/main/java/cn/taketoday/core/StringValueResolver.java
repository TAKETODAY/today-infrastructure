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

package cn.taketoday.core;

import cn.taketoday.lang.Nullable;

/**
 * Simple strategy interface for resolving a String value.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/9/30 22:44
 * @see MetadataResolver
 * @since 4.0
 */
@FunctionalInterface
public interface StringValueResolver {

  /**
   * Resolve the given String value, for example parsing placeholders.
   *
   * @param strVal the original String value (never {@code null})
   * @return the resolved String value (may be {@code null} when resolved to a null
   * value), possibly the original String value itself (in case of no placeholders
   * to resolve or when ignoring unresolvable placeholders)
   * @throws IllegalArgumentException in case of an unresolvable String value
   */
  @Nullable
  String resolveStringValue(String strVal);

}
