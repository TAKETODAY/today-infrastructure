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

import infra.lang.VisibleForTesting;
import infra.util.ObjectUtils;

/**
 * read-only PropertyAccessor
 *
 * @author TODAY 2020/9/12 15:22
 * @see ReflectionException
 */
public abstract class ReadOnlyPropertyAccessor extends PropertyAccessor {

  @Override
  public final void set(Object obj, @Nullable Object value) {
    throwReadonly(obj, value);
  }

  @Override
  public final Method getWriteMethod() {
    throw new ReflectionException("Readonly property");
  }

  @Override
  public final boolean isWriteable() {
    return false;
  }

  @Nullable
  @VisibleForTesting
  static Class<?> classToString(@Nullable Object obj) {
    return obj != null ? obj.getClass() : null;
  }

  static void throwReadonly(Object obj, @Nullable Object value) {
    throw new ReflectionException("Can't set value '%s' to '%s' read only property"
            .formatted(ObjectUtils.toHexString(value), classToString(obj)));
  }

}
