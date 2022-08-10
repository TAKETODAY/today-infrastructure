/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core;

import java.io.Serial;

import cn.taketoday.lang.Nullable;

/**
 * not a suitable Constructor found
 *
 * @author TODAY 2021/8/23 23:28
 * @since 4.0
 */
public class ConstructorNotFoundException extends NestedRuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;
  private final Class<?> type;
  @Nullable
  private final Class<?>[] parameterTypes;

  public ConstructorNotFoundException(Class<?> type) {
    this(type, "No suitable constructor in class: " + type);
  }

  public ConstructorNotFoundException(Class<?> type, String msg) {
    this(type, msg, null, null);
  }

  public ConstructorNotFoundException(Class<?> type, Class<?>[] parameterTypes, Throwable e) {
    this(type, "No suitable constructor in class: " + type, parameterTypes, e);
  }

  public ConstructorNotFoundException(
          Class<?> type, String msg, @Nullable Class<?>[] parameterTypes, @Nullable Throwable e) {
    super(msg, e);
    this.type = type;
    this.parameterTypes = parameterTypes;
  }

  public Class<?> getType() {
    return type;
  }

  @Nullable
  public Class<?>[] getParameterTypes() {
    return parameterTypes;
  }

}
