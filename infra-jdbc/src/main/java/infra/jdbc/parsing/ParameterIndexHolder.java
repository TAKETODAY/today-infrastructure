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

package infra.jdbc.parsing;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import infra.jdbc.ParameterBinder;

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
