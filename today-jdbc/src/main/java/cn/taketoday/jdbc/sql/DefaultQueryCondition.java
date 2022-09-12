/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jdbc.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.jdbc.type.ObjectTypeHandler;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/10 21:34
 */
public class DefaultQueryCondition extends QueryCondition {

  private ObjectTypeHandler typeHandler = ObjectTypeHandler.getSharedInstance();

  private final String columnName;

  private final Operator operator;

  @Nullable
  private final Object parameterValue; // Object, array, list

  private final int valueLength;

  public DefaultQueryCondition(String columnName, Operator operator, @Nullable Object parameterValue) {
    Assert.notNull(operator, "operator is required");
    Assert.notNull(columnName, "columnName is required");
    this.parameterValue = parameterValue;
    this.operator = operator;
    this.columnName = columnName;
    this.valueLength = getLength(parameterValue);
  }

  public void setTypeHandler(ObjectTypeHandler typeHandler) {
    Assert.notNull(typeHandler, "typeHandler is required");
    this.typeHandler = typeHandler;
  }

  @Override
  protected boolean matches() {
    return parameterValue != null; // TODO
  }

  /**
   * @param ps PreparedStatement
   * @throws SQLException if parameterIndex does not correspond to a parameter
   * marker in the SQL statement; if a database access error occurs;
   * this method is called on a closed {@code PreparedStatement}
   * or the type of the given object is ambiguous
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void setParameterInternal(PreparedStatement ps) throws SQLException {
    if (valueLength != 1) {
      final int position = this.position;
      final Object parameterValue = this.parameterValue;
      if (parameterValue instanceof Object[] array) {
        for (int i = 0; i < valueLength; i++) {
          typeHandler.setParameter(ps, position + i, array[i]);
        }
      }
      else if (parameterValue != null) {
        int i = 0;
        for (Object parameter : (Iterable<Object>) parameterValue) {
          typeHandler.setParameter(ps, position + i++, parameter);
        }
      }
    }
    else {
      typeHandler.setParameter(ps, position, parameterValue);
    }
  }

  @Override
  protected void renderInternal(StringBuilder sql) {
    // column_name
    sql.append(" `");
    sql.append(columnName);
    sql.append("`");

    // operator and value

    operator.render(sql, parameterValue, valueLength);
  }

  @Override
  protected void setNextNodePosition(QueryCondition next) {
    if (next instanceof DefaultQueryCondition nextCondition) {
      nextCondition.updatePosition(valueLength);
    }
    else {
      super.setNextNodePosition(next);
    }
  }

  //

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof DefaultQueryCondition that))
      return false;
    return Objects.equals(typeHandler, that.typeHandler)
            && Objects.equals(columnName, that.columnName)
            && Objects.equals(parameterValue, that.parameterValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(typeHandler, columnName, parameterValue);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("columnName", columnName)
            .append("value", parameterValue)
            .toString();
  }

}
