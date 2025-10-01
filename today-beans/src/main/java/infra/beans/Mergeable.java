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

package infra.beans;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.support.ManagedList;
import infra.beans.factory.support.ManagedMap;
import infra.beans.factory.support.ManagedProperties;
import infra.beans.factory.support.ManagedSet;

/**
 * Interface representing an object whose value set can be merged with
 * that of a parent object.
 *
 * @author Rob Harrop
 * @see ManagedSet
 * @see ManagedList
 * @see ManagedMap
 * @see ManagedProperties
 * @since 4.0
 */
public interface Mergeable {

  /**
   * Is merging enabled for this particular instance?
   */
  boolean isMergeEnabled();

  /**
   * Merge the current value set with that of the supplied object.
   * <p>The supplied object is considered the parent, and values in
   * the callee's value set must override those of the supplied object.
   *
   * @param parent the object to merge with
   * @return the result of the merge operation
   * @throws IllegalArgumentException if the supplied parent is {@code null}
   * @throws IllegalStateException if merging is not enabled for this instance
   * (i.e. {@code mergeEnabled} equals {@code false}).
   */
  Object merge(@Nullable Object parent);

}
