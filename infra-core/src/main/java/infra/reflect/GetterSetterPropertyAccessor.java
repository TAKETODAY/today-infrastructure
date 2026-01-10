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
 * @author TODAY
 * 2020/9/11 17:27
 */
final class GetterSetterPropertyAccessor extends PropertyAccessor {

  private final GetterMethod readMethod;
  private final SetterMethod writeMethod;

  GetterSetterPropertyAccessor(GetterMethod readMethod, SetterMethod writeMethod) {
    this.readMethod = readMethod;
    this.writeMethod = writeMethod;
  }

  @Nullable
  @Override
  public Object get(final Object obj) {
    return readMethod.get(obj);
  }

  @Override
  public void set(final Object obj, @Nullable Object value) {
    writeMethod.set(obj, value);
  }

  @Nullable
  @Override
  public Method getReadMethod() {
    return readMethod.getReadMethod();
  }

  @Nullable
  @Override
  public Method getWriteMethod() {
    return writeMethod.getWriteMethod();
  }

}
