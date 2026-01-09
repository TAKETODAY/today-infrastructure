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

import java.lang.reflect.Constructor;

import infra.reflect.Accessor;

/**
 * @author TODAY 2021/8/27 21:41
 */
public abstract class ConstructorAccessor extends BeanInstantiator implements Accessor {

  protected final Constructor<?> constructor;

  public ConstructorAccessor(Constructor<?> constructor) {
    this.constructor = constructor;
  }

  @Override
  public Constructor<?> getConstructor() {
    return constructor;
  }

  /**
   * Invoke {@link java.lang.reflect.Constructor} with given args
   *
   * @return returns Object
   * @since 4.0
   */
  @Override
  public abstract Object doInstantiate(@Nullable Object @Nullable [] args);
}
