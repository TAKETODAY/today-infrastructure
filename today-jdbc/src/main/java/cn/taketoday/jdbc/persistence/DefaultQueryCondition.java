/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.jdbc.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.jdbc.type.ObjectTypeHandler;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/10 21:34
 */
public class DefaultQueryCondition extends QueryCondition {

  protected final boolean nullable;

  @SuppressWarnings({ "rawtypes" })
  protected TypeHandler typeHandler = ObjectTypeHandler.sharedInstance;

  protected final String columnName;

  protected final Operator operator;

  @Nullable
  protected final Object parameterValue; // Object, array, list

  protected final int valueLength;

  public DefaultQueryCondition(String columnName,
          Operator operator, @Nullable Object parameterValue) {
    this(columnName, operator, parameterValue, false);
  }

  /**
   * @param nullable parameter-value match null
   */
  public DefaultQueryCondition(String columnName,
          Operator operator, @Nullable Object parameterValue, boolean nullable) {
    Assert.notNull(operator, "operator is required");
    Assert.notNull(columnName, "columnName is required");
    this.parameterValue = parameterValue;
    this.operator = operator;
    this.columnName = columnName;
    this.valueLength = getLength(parameterValue);
    this.nullable = nullable;
  }

  public void setTypeHandler(TypeHandler<?> typeHandler) {
    Assert.notNull(typeHandler, "typeHandler is required");
    this.typeHandler = typeHandler;
  }

  @Override
  protected boolean matches() {
    return nullable || parameterValue != null; // TODO
  }

  /**
   * @param ps PreparedStatement
   * @param idx current parameter-index
   * @throws SQLException if parameterIndex does not correspond to a parameter
   * marker in the SQL statement; if a database access error occurs;
   * this method is called on a closed {@code PreparedStatement}
   * or the type of the given object is ambiguous
   */
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected int setParameterInternal(PreparedStatement ps, int idx) throws SQLException {
    int valueLength = this.valueLength;
    if (valueLength != 1) {
      // array, collection
      final Object parameterValue = this.parameterValue;
      final TypeHandler typeHandler = this.typeHandler;
      if (parameterValue instanceof Object[] array) {
        for (int i = 0; i < valueLength; i++) {
          typeHandler.setParameter(ps, idx + i, array[i]);
        }
      }
      else if (parameterValue != null) {
        int i = idx;
        for (Object parameter : (Iterable<Object>) parameterValue) {
          typeHandler.setParameter(ps, i++, parameter);
        }
      }
      return idx + valueLength;
    }
    else {
      Object parameterValue = this.parameterValue;
      if (parameterValue instanceof Collection<?> coll) {
        parameterValue = CollectionUtils.firstElement(coll);
      }
      else if (parameterValue instanceof Object[] array) {
        parameterValue = array[0];
      }
      typeHandler.setParameter(ps, idx, parameterValue);
      return idx + 1;
    }
  }

  @Override
  protected void renderInternal(StringBuilder sql) {
    // column_name
    sql.append(" `");
    sql.append(columnName);
    sql.append('`');

    // operator and value

    operator.render(sql, parameterValue, valueLength);
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
