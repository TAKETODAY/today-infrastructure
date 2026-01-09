/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.beans.support;

import org.jspecify.annotations.Nullable;

import infra.lang.Assert;
import infra.util.CollectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CollectionUtils#createCollection(Class, Class, int)
 * @since 3.0 2021/1/29 15:56
 */
public class CollectionInstantiator extends BeanInstantiator {

  @Nullable
  private final Class<?> elementType;

  private final Class<?> collectionType;

  public CollectionInstantiator(Class<?> collectionType) {
    this(collectionType, null);
  }

  public CollectionInstantiator(Class<?> collectionType, @Nullable Class<?> elementType) {
    Assert.notNull(collectionType, "collection type is required");
    this.elementType = elementType;
    this.collectionType = collectionType;
  }

  @Override
  public Object doInstantiate(final @Nullable Object @Nullable [] args) {
    return CollectionUtils.createCollection(collectionType, elementType, 0);
  }

}
