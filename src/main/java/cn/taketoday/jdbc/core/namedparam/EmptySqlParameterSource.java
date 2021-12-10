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

package cn.taketoday.jdbc.core.namedparam;

import cn.taketoday.lang.Nullable;

/**
 * A simple empty implementation of the {@link SqlParameterSource} interface.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class EmptySqlParameterSource implements SqlParameterSource {

  /**
   * A shared instance of {@link EmptySqlParameterSource}.
   */
  public static final EmptySqlParameterSource INSTANCE = new EmptySqlParameterSource();

  @Override
  public boolean hasValue(String paramName) {
    return false;
  }

  @Override
  @Nullable
  public Object getValue(String paramName) throws IllegalArgumentException {
    throw new IllegalArgumentException("This SqlParameterSource is empty");
  }

  @Override
  public int getSqlType(String paramName) {
    return TYPE_UNKNOWN;
  }

  @Override
  @Nullable
  public String getTypeName(String paramName) {
    return null;
  }

  @Override
  @Nullable
  public String[] getParameterNames() {
    return null;
  }

}
