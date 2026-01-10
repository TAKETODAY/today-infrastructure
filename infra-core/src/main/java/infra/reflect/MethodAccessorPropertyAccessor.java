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

/**
 * MethodInvoker PropertyAccessor implementation
 *
 * @author TODAY 2020/9/11 15:54
 */
final class MethodAccessorPropertyAccessor extends PropertyAccessor {
  private final MethodInvoker readAccessor;
  private final MethodInvoker writeAccessor;

  MethodAccessorPropertyAccessor(MethodInvoker readAccessor, MethodInvoker writeAccessor) {
    this.readAccessor = readAccessor;
    this.writeAccessor = writeAccessor;
  }

  @Nullable
  @Override
  public Object get(final Object obj) {
    return readAccessor.invoke(obj, null);
  }

  @Override
  public void set(Object obj, @Nullable Object value) {
    writeAccessor.invoke(obj, new Object[] { value });
  }

  @Override
  public Method getReadMethod() {
    return readAccessor.getMethod();
  }

  @Override
  public Method getWriteMethod() {
    return writeAccessor.getMethod();
  }
}
