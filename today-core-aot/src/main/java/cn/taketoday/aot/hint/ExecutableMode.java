/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.hint;

import java.lang.reflect.Executable;

import cn.taketoday.lang.Nullable;

/**
 * Represent the need of reflection for a given {@link Executable}.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public enum ExecutableMode {

  /**
   * Only retrieving the {@link Executable} and its metadata is required.
   */
  INTROSPECT,

  /**
   * Full reflection support is required, including the ability to invoke
   * the {@link Executable}.
   */
  INVOKE;

  /**
   * Specify if this mode already includes the specified {@code other} mode.
   *
   * @param other the other mode to check
   * @return {@code true} if this mode includes the other mode
   */
  boolean includes(@Nullable ExecutableMode other) {
    return (other == null || this.ordinal() >= other.ordinal());
  }

}
