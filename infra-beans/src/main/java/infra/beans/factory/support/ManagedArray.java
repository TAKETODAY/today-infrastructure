/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.factory.support;

import org.jspecify.annotations.Nullable;

import infra.lang.Assert;

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
