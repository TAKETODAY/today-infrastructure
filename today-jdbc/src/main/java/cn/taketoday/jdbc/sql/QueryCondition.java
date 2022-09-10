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

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.jdbc.type.ObjectTypeHandler;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/10 21:34
 */
public class QueryCondition {

  private ObjectTypeHandler typeHandler = ObjectTypeHandler.getSharedInstance();

  private final String columnName;

  private final Operator operator;

  @Nullable
  private final Object value; // Object, array, list

  private int position = 1;

  private final int valueLength;

  @Nullable
  private String type;

  @Nullable
  private QueryCondition nextNode;

  private QueryCondition preNode;

  public QueryCondition(String columnName, Operator operator, @Nullable Object value) {
    Assert.notNull(operator, "operator is required");
    Assert.notNull(columnName, "columnName is required");
    this.value = value;
    this.operator = operator;
    this.columnName = columnName;
    this.valueLength = getLength(value);
  }

  private static int getLength(@Nullable Object value) {
    if (ObjectUtils.isArray(value)) {
      return Array.getLength(value);
    }
    else if (value instanceof Collection<?> collection) {
      return collection.size();
    }
    else {
      return 1;
    }
  }

  public void setTypeHandler(ObjectTypeHandler typeHandler) {
    Assert.notNull(typeHandler, "typeHandler is required");
    this.typeHandler = typeHandler;
  }

  protected boolean matches() {
    return value != null; // TODO
  }

  /**
   * <p>Sets the value of the designated parameter using the given object.
   *
   * <p>The JDBC specification specifies a standard mapping from
   * Java {@code Object} types to SQL types.  The given argument
   * will be converted to the corresponding SQL type before being
   * sent to the database.
   *
   * <p>Note that this method may be used to pass database-
   * specific abstract data types, by using a driver-specific Java
   * type.
   *
   * If the object is of a class implementing the interface {@code SQLData},
   * the JDBC driver should call the method {@code SQLData.writeSQL}
   * to write it to the SQL data stream.
   * If, on the other hand, the object is of a class implementing
   * {@code Ref}, {@code Blob}, {@code Clob},  {@code NClob},
   * {@code Struct}, {@code java.net.URL}, {@code RowId}, {@code SQLXML}
   * or {@code Array}, the driver should pass it to the database as a
   * value of the corresponding SQL type.
   * <P>
   * <b>Note:</b> Not all databases allow for a non-typed Null to be sent to
   * the backend. For maximum portability, the {@code setNull} or the
   * {@code setObject(int parameterIndex, Object x, int sqlType)}
   * method should be used
   * instead of {@code setObject(int parameterIndex, Object x)}.
   * <p>
   * <b>Note:</b> This method throws an exception if there is an ambiguity, for example, if the
   * object is of a class implementing more than one of the interfaces named above.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @throws SQLException if parameterIndex does not correspond to a parameter
   * marker in the SQL statement; if a database access error occurs;
   * this method is called on a closed {@code PreparedStatement}
   * or the type of the given object is ambiguous
   */
  public void setParameter(PreparedStatement ps, int parameterIndex) throws SQLException {
    typeHandler.setParameter(ps, parameterIndex, value);
  }

  /**
   * @param ps PreparedStatement
   * @throws SQLException if parameterIndex does not correspond to a parameter
   * marker in the SQL statement; if a database access error occurs;
   * this method is called on a closed {@code PreparedStatement}
   * or the type of the given object is ambiguous
   */
  public void setParameter(PreparedStatement ps) throws SQLException {
    typeHandler.setParameter(ps, position, value);

    if (nextNode != null) {
      nextNode.setParameter(ps);
    }
  }

  /**
   * append sql
   *
   * @param sql sql append to
   */
  public void render(StringBuilder sql) {
    if (matches()) {
      if (preNode != null && type != null) {
        // not first condition
        sql.append(' ');
        sql.append(type);
      }

      sql.append(" `");
      sql.append(columnName);
      sql.append("`");

      operator.render(sql, value, valueLength);

      if (nextNode != null) {
        nextNode.render(sql);
      }
    }
  }

  public QueryCondition and(QueryCondition next) {
    next.type = "AND";
    setNext(next);
    return this;
  }

  public QueryCondition or(QueryCondition next) {
    next.type = "OR";
    setNext(next);
    return this;
  }

  private void setNext(QueryCondition next) {
    this.nextNode = next;
    next.position = position + 1;
    next.preNode = this;
  }

  // Static factory methods

  public static QueryCondition of(String columnName, Operator operator, Object value) {
    return new QueryCondition(columnName, operator, value);
  }

  public static QueryCondition equalsTo(String columnName, Object value) {
    return new QueryCondition(columnName, Operator.EQUALS, value);
  }

  public static QueryCondition between(String columnName, Object array) {
    Assert.isTrue(getLength(array) == 2, "BETWEEN expression must have left and right value");
    return new QueryCondition(columnName, Operator.BETWEEN, array);
  }

  public static QueryCondition between(String columnName, Object left, Object right) {
    return new QueryCondition(columnName, Operator.BETWEEN, new Object[] { left, right });
  }

  public static QueryCondition notBetween(String columnName, Object array) {
    Assert.isTrue(getLength(array) == 2, "BETWEEN expression must have left and right value");
    return new QueryCondition(columnName, Operator.BETWEEN, array);
  }

  public static QueryCondition notBetween(String columnName, Object left, Object right) {
    return new QueryCondition(columnName, Operator.NOT_BETWEEN, new Object[] { left, right });
  }

  public static QueryCondition isNull(String columnName) {
    return new QueryCondition(columnName, Operator.IS_NULL, null) {
      @Override
      public boolean matches() {
        return true;
      }
    };
  }

  public static QueryCondition nullable(String columnName, Object value) {
    return new QueryCondition(columnName, Operator.EQUALS, value) {
      @Override
      public boolean matches() {
        return true;
      }
    };
  }

  public static QueryCondition notEmpty(String columnName, Object value) {
    return new QueryCondition(columnName, Operator.EQUALS, value) {
      @Override
      public boolean matches() {
        return ObjectUtils.isNotEmpty(value);
      }
    };
  }

  //

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof QueryCondition that))
      return false;
    return Objects.equals(typeHandler, that.typeHandler)
            && Objects.equals(columnName, that.columnName)
            && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(typeHandler, columnName, value);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("columnName", columnName)
            .append("value", value)
            .toString();
  }

}
