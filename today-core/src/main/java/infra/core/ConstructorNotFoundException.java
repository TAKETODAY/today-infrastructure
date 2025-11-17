/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
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
    this(type, "No suitable constructor in class: " + type);
  }

  public ConstructorNotFoundException(Class<?> type, String msg) {
    this(type, msg, null, null);
  }

  public ConstructorNotFoundException(Class<?> type, Class<?> @Nullable [] parameterTypes, Throwable e) {
    this(type, "No suitable constructor in class: " + type, parameterTypes, e);
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
