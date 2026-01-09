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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import infra.lang.Assert;
import infra.util.ReflectionUtils;

/**
 * @author TODAY 2020/9/18 22:03
 */
final class ReflectiveReadOnlyPropertyAccessor extends ReadOnlyPropertyAccessor {

  @Nullable
  private final Field field;

  @Nullable
  private final Method readMethod;

  ReflectiveReadOnlyPropertyAccessor(@Nullable Field field, @Nullable Method readMethod) {
    Assert.isTrue(field != null || readMethod != null,
            "field or read method must be specified one");
    this.field = ReflectionUtils.makeAccessible(field);
    this.readMethod = ReflectionUtils.makeAccessible(readMethod);
  }

  @Nullable
  @Override
  @SuppressWarnings("NullAway")
  public Object get(final Object obj) {
    if (readMethod != null) {
      return ReflectionUtils.invokeMethod(readMethod, obj);
    }
    return ReflectionUtils.getField(field, obj);
  }

  @Nullable
  @Override
  public Method getReadMethod() {
    return readMethod;
  }
}
