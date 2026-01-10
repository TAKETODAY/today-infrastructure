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

import infra.util.ReflectionUtils;

/**
 * @author TODAY 2020/9/19 22:38
 * @see ReflectionUtils#setField(Field, Object, Object)
 */
final class ReflectiveSetterMethod implements SetterMethod {
  private final Field field;

  public ReflectiveSetterMethod(final Field field) {
    this.field = field;
  }

  @Override
  public void set(Object obj, @Nullable Object value) {
    ReflectionUtils.setField(field, obj, value);
  }
}
