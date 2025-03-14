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

package infra.beans.factory.support;

import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Tag collection class used to hold managed array elements, which may
 * include runtime bean references (to be resolved into bean objects).
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ManagedArray extends ManagedList<Object> {

  /** Resolved element type for runtime creation of the target array. */
  @Nullable
  volatile Class<?> resolvedElementType;

  /**
   * Create a new managed array placeholder.
   *
   * @param elementTypeName the target element type as a class name
   * @param size the size of the array
   */
  public ManagedArray(String elementTypeName, int size) {
    super(size);
    Assert.notNull(elementTypeName, "elementTypeName is required");
    setElementTypeName(elementTypeName);
  }

}
