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

import infra.reflect.MethodAccessor;

/**
 * @author TODAY 2020/9/20 20:35
 */
class StaticMethodAccessorBeanInstantiator extends BeanInstantiator {
  protected final MethodAccessor accessor;

  StaticMethodAccessorBeanInstantiator(final MethodAccessor accessor) {
    this.accessor = accessor;
  }

  @Override
  @SuppressWarnings("NullAway")
  public final Object doInstantiate(final @Nullable Object @Nullable [] args) {
    return accessor.invoke(getObject(), args);
  }

  @Nullable
  protected Object getObject() {
    return null;
  }

  @Override
  public String toString() {
    return "BeanInstantiator for static method: " + accessor.getMethod();
  }

}
