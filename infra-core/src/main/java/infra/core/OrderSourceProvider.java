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

package infra.core;

import org.jspecify.annotations.Nullable;

/**
 * Strategy interface to provide an order source for a given object.
 *
 * @author TODAY 2021/9/12 11:34
 * @since 4.0
 */
@FunctionalInterface
public interface OrderSourceProvider {

  /**
   * Return an order source for the specified object, i.e. an object that
   * should be checked for an order value as a replacement to the given object.
   * <p>Can also be an array of order source objects.
   * <p>If the returned object does not indicate any order, the comparator
   * will fall back to checking the original object.
   *
   * @param obj the object to find an order source for
   * @return the order source for that object, or {@code null} if none found
   */
  @Nullable
  Object getOrderSource(Object obj);
}
