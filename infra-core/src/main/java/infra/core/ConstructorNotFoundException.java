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

package infra.core;

import org.jspecify.annotations.Nullable;

/**
 * not a suitable Constructor found
 *
 * @author TODAY 2021/8/23 23:28
 * @since 4.0
 */
public class ConstructorNotFoundException extends NestedRuntimeException {

  private final Class<?> type;

  private final Class<?> @Nullable [] parameterTypes;

  public ConstructorNotFoundException(Class<?> type) {
    this(type, "No suitable constructor in " + type);
  }

  public ConstructorNotFoundException(Class<?> type, String msg) {
    this(type, msg, null, null);
  }

  public ConstructorNotFoundException(Class<?> type, Class<?> @Nullable [] parameterTypes, Throwable e) {
    this(type, "No suitable constructor in " + type, parameterTypes, e);
  }

  public ConstructorNotFoundException(Class<?> type, String msg, Class<?> @Nullable [] parameterTypes, @Nullable Throwable e) {
    super(msg, e);
    this.type = type;
    this.parameterTypes = parameterTypes;
  }

  public Class<?> getType() {
    return type;
  }

  public Class<?> @Nullable [] getParameterTypes() {
    return parameterTypes;
  }

}
