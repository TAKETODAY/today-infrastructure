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

package cn.taketoday.expression;

import cn.taketoday.lang.Nullable;

/**
 * Strategy for types that access elements of specific target classes.
 *
 * <p>This interface places no restrictions on what constitutes an element.
 *
 * <p>A targeted accessor can specify a set of target classes for which it should
 * be called. However, if it returns {@code null} or an empty array from
 * {@link #getSpecificTargetClasses()}, it will typically be called for all
 * access operations and given a chance to determine if it supports a concrete
 * access attempt.
 *
 * <p>Targeted accessors are considered to be ordered, and each will be called
 * in turn. The only rule that affects the call order is that any accessor which
 * specifies explicit support for a given target class via
 * {@link #getSpecificTargetClasses()} will be called first, before other generic
 * accessors that do not specify explicit support for the given target class.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertyAccessor
 * @see IndexAccessor
 * @since 4.0
 */
public interface TargetedAccessor {

  /**
   * Get the set of classes for which this accessor should be called.
   * <p>Returning {@code null} or an empty array indicates this is a generic
   * accessor that can be called in an attempt to access an element on any
   * type.
   *
   * @return an array of classes that this accessor is suitable for
   * (or {@code null} or an empty array if a generic accessor)
   */
  @Nullable
  Class<?>[] getSpecificTargetClasses();

}
