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

package infra.reflect;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

import infra.util.ExceptionUtils;
import infra.util.ReflectionUtils;

/**
 * java reflect {@link Method} implementation
 *
 * @author TODAY  2020/9/20 21:49
 */
final class ReflectiveMethodAccessor extends MethodInvoker implements MethodAccessor {

  // @since 4.0
  private final boolean handleReflectionException;

  ReflectiveMethodAccessor(final Method method, boolean handleReflectionException) {
    super(method);
    this.handleReflectionException = handleReflectionException;
  }

  @Nullable
  @Override
  public Object invoke(@Nullable Object obj, final @Nullable Object @Nullable [] args) {
    try {
      return getMethod().invoke(obj, args);
    }
    catch (Exception e) {
      if (handleReflectionException) {
        ReflectionUtils.handleReflectionException(e);
      }
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

}
