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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/11 21:51
 */
public abstract class QueryCondition {

  // parameter start position
  protected int position = 1;

  @Nullable
  protected String type;

  @Nullable
  protected QueryCondition nextNode;

  @Nullable
  protected QueryCondition preNode;

  protected abstract boolean matches();

  /**
   * @param ps PreparedStatement
   * @throws SQLException if parameterIndex does not correspond to a parameter
   * marker in the SQL statement; if a database access error occurs;
   * this method is called on a closed {@code PreparedStatement}
   * or the type of the given object is ambiguous
   */
  protected void setParameter(PreparedStatement ps) throws SQLException {
    setParameterInternal(ps);

    if (nextNode != null) {
      nextNode.setParameter(ps);
    }
  }

  protected abstract void setParameterInternal(PreparedStatement ps) throws SQLException;

  /**
   * append sql
   *
   * @param sql sql append to
   */
  public boolean render(StringBuilder sql) {
    if (matches()) {
      // format: column_name operator value;
      if (preNode != null && type != null) {
        // not first condition
        sql.append(' ');
        sql.append(type);
      }

      renderInternal(sql);

      if (nextNode != null) {
        nextNode.render(sql);
      }
      return true;
    }
    return false;
  }

  protected abstract void renderInternal(StringBuilder sql);

  /**
   * <p>
   * This Method should use once
   *
   * @param next Next condition
   * @return this
   */
  public QueryCondition and(QueryCondition next) {
    next.type = "AND";
    setNext(next);
    return this;
  }

  /**
   * <p>
   * This Method should use once
   *
   * @param next Next condition
   * @return this
   */
  public QueryCondition or(QueryCondition next) {
    next.type = "OR";
    setNext(next);
    return this;
  }

  protected void setNext(QueryCondition next) {
    setNextNodePosition(next);
    this.nextNode = next;
    next.preNode = this;
  }

  protected void updatePosition(int basePosition) {
    this.position = this.position + basePosition;
    if (nextNode != null) {
      nextNode.updatePosition(basePosition);
    }
  }

  protected int getLastPosition() {
    if (nextNode != null) {
      return nextNode.getLastPosition();
    }
    return position;
  }

  protected void setNextNodePosition(QueryCondition next) {
    next.position = position + 1;
  }

  // Static factory methods

  public static DefaultQueryCondition of(String columnName, Operator operator, Object value) {
    return new DefaultQueryCondition(columnName, operator, value);
  }

  public static DefaultQueryCondition equalsTo(String columnName, Object value) {
    return new DefaultQueryCondition(columnName, Operator.EQUALS, value);
  }

  public static DefaultQueryCondition between(String columnName, Object array) {
    Assert.isTrue(getLength(array) == 2, "BETWEEN expression must have left and right value");
    return new DefaultQueryCondition(columnName, Operator.BETWEEN, array);
  }

  public static DefaultQueryCondition between(String columnName, Object left, Object right) {
    return new DefaultQueryCondition(columnName, Operator.BETWEEN, new Object[] { left, right });
  }

  public static DefaultQueryCondition notBetween(String columnName, Object array) {
    Assert.isTrue(getLength(array) == 2, "BETWEEN expression must have left and right value");
    return new DefaultQueryCondition(columnName, Operator.BETWEEN, array);
  }

  public static DefaultQueryCondition notBetween(String columnName, Object left, Object right) {
    return new DefaultQueryCondition(columnName, Operator.NOT_BETWEEN, new Object[] { left, right });
  }

  public static DefaultQueryCondition isNull(String columnName) {
    return new DefaultQueryCondition(columnName, Operator.IS_NULL, null) {
      @Override
      public boolean matches() {
        return true;
      }
    };
  }

  public static DefaultQueryCondition nullable(String columnName, Object value) {
    return new DefaultQueryCondition(columnName, Operator.EQUALS, value) {
      @Override
      public boolean matches() {
        return true;
      }
    };
  }

  public static DefaultQueryCondition notEmpty(String columnName, Object value) {
    return new DefaultQueryCondition(columnName, Operator.EQUALS, value) {
      @Override
      public boolean matches() {
        return ObjectUtils.isNotEmpty(value);
      }
    };
  }

  /**
   * <pre>
   *   {@code
   *     QueryCondition condition = QueryCondition.nested(
   *             QueryCondition.equalsTo("name", "TODAY")
   *                     .or(QueryCondition.equalsTo("age", 10))
   *     ).and(
   *             QueryCondition.nested(
   *                     QueryCondition.equalsTo("gender", Gender.MALE)
   *                             .and(QueryCondition.of("email", Operator.PREFIX_LIKE, "taketoday"))
   *             )
   *     );
   *
   *    // will produce SQL:
   *     ( `name` = ? OR `age` = ? ) AND ( `gender` = ? AND `email` like concat(?, '%') )
   *
   *   }
   * </pre>
   *
   * @param condition normal condition
   * @return nested condition
   */
  public static NestedQueryCondition nested(QueryCondition condition) {
    return new NestedQueryCondition(condition);
  }

  //

  static int getLength(@Nullable Object value) {
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
}
