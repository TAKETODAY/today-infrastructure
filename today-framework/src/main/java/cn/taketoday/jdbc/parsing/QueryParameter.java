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
import java.util.Objects;

import cn.taketoday.jdbc.ParameterBinder;

/**
 * optimized Query-Parameter resolving
 *
 * @author TODAY 2021/8/22 10:15
 * @since 4.0
 */
public final class QueryParameter {
  private final String name;

  private ParameterBinder setter;
  private ParameterIndexHolder applier;

  public QueryParameter(String name, ParameterIndexHolder indexHolder) {
    this.name = name;
    this.applier = indexHolder;
  }

  /**
   * set value to given statement
   *
   * @param statement statement
   * @throws SQLException any parameter setting error
   */
  public void setTo(final PreparedStatement statement) throws SQLException {
    if (setter != null) {
      applier.bind(setter, statement);
    }
  }

  public void setHolder(ParameterIndexHolder applier) {
    this.applier = applier;
  }

  public void setSetter(ParameterBinder setter) {
    this.setter = setter;
  }

  public ParameterIndexHolder getHolder() {
    return applier;
  }

  public ParameterBinder getBinder() {
    return setter;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final QueryParameter parameter))
      return false;
    return Objects.equals(name, parameter.name)
            && Objects.equals(setter, parameter.setter)
            && Objects.equals(applier, parameter.applier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, setter, applier);
  }

  @Override
  public String toString() {
    return "QueryParameter: '" + name + "' setter: " + setter;
  }

}
