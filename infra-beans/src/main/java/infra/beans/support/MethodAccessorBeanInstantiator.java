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

import java.util.function.Supplier;

import infra.reflect.MethodAccessor;
import infra.util.function.SingletonSupplier;

/**
 * @author TODAY 2020/9/20 20:41
 */
final class MethodAccessorBeanInstantiator
        extends StaticMethodAccessorBeanInstantiator {

  private final Supplier<Object> obj;

  MethodAccessorBeanInstantiator(MethodAccessor accessor, Object obj) {
    this(accessor, SingletonSupplier.of(obj));
  }

  MethodAccessorBeanInstantiator(MethodAccessor accessor, Supplier<Object> obj) {
    super(accessor);
    this.obj = obj;
  }

  @Override
  protected Object getObject() {
    return obj.get();
  }

  @Override
  public String toString() {
    return "BeanInstantiator for instance method: " + accessor.getMethod();
  }
}
