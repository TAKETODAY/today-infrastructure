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

package cn.taketoday.util;

import cn.taketoday.lang.Nullable;

/**
 * Strategy interface used to resolve replacement values for placeholders contained in Strings.
 *
 * @author TODAY 2021/10/3 13:24
 */
@FunctionalInterface
public interface PlaceholderResolver {

  /**
   * Resolve the supplied placeholder name to the replacement value.
   *
   * @param placeholderName the name of the placeholder to resolve
   * @return the replacement value, or {@code null} if no replacement is to be made
   */
  @Nullable
  String resolvePlaceholder(String placeholderName);
}
