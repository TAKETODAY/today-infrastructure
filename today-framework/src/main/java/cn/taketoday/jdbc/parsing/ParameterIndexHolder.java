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

package cn.taketoday.jdbc.parsing;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import cn.taketoday.jdbc.ParameterBinder;

/**
 * parameter index holder
 *
 * @author TODAY 2021/6/8 23:51
 * @since 4.0
 */
public abstract class ParameterIndexHolder implements Iterable<Integer> {

  /**
   * use binder to bind parameter to this index where there is hold
   *
   * @param binder parameter setter set to statement
   * @param statement target PreparedStatement
   * @throws SQLException any parameter setting error
   */
  public abstract void bind(ParameterBinder binder, PreparedStatement statement)
          throws SQLException;

  //---------------------------------------------------------------------
  // Implementation of Iterable interface
  //---------------------------------------------------------------------

  @Override
  public abstract Iterator<Integer> iterator();

  // static

  public static ParameterIndexHolder valueOf(int index) {
    return new DefaultParameterIndexHolder(index);
  }

  public static ParameterIndexHolder valueOf(List<Integer> indices) {
    return new ListParameterIndexApplier(indices);
  }

}
