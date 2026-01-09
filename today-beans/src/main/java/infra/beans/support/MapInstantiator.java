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
 * @author TODAY 2021/1/29 15:56
 * @see CollectionUtils#createMap(Class, Class, int)
 * @since 3.0
 */
public class MapInstantiator extends BeanInstantiator {

  @Nullable
  private final Class<?> keyType;

  private final Class<?> mapType;

  public MapInstantiator(Class<?> mapType) {
    this(mapType, null);
  }

  public MapInstantiator(Class<?> mapType, @Nullable Class<?> keyType) {
    Assert.notNull(mapType, "map type is required");
    this.keyType = keyType;
    this.mapType = mapType;
  }

  @Override
  public Object doInstantiate(final @Nullable Object @Nullable [] args) {
    return CollectionUtils.createMap(mapType, keyType, 0);
  }

}
