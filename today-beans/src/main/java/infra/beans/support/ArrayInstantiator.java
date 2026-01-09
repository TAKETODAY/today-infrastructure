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

import java.lang.reflect.Array;

import infra.lang.Assert;

/**
 * @author TODAY 2021/1/29 15:56
 * @see Array#newInstance(Class, int)
 * @since 3.0
 */
public class ArrayInstantiator extends BeanInstantiator {

  private final Class<?> componentType;

  public ArrayInstantiator(Class<?> componentType) {
    Assert.notNull(componentType, "component type is required");
    this.componentType = componentType;
  }

  @Override
  public Object doInstantiate(final @Nullable Object @Nullable [] args) {
    // TODO - only handles 2-dimensional arrays
    final Class<?> componentType = this.componentType;
    if (componentType.isArray()) {
      Object array = Array.newInstance(componentType, 1);
      Array.set(array, 0, Array.newInstance(componentType.getComponentType(), 0));
      return array;
    }
    else {
      return Array.newInstance(componentType, 0);
    }
  }

}
