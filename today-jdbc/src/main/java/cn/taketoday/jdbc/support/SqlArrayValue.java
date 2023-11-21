/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jdbc.support;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Common {@link SqlValue} implementation for JDBC {@link Array} creation
 * based on the JDBC 4 {@link java.sql.Connection#createArrayOf} method.
 *
 * <p>Also serves as a template for custom {@link SqlValue} implementations
 * with cleanup demand.
 *
 * @author Juergen Hoeller
 * @author Philippe Marschall
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SqlArrayValue implements SqlValue {

  private final String typeName;

  private final Object[] elements;

  @Nullable
  private Array array;

  /**
   * Create a new {@code SqlArrayValue} for the given type name and elements.
   *
   * @param typeName the SQL name of the type the elements of the array map to
   * @param elements the elements to populate the {@code Array} object with
   * @see java.sql.Connection#createArrayOf
   */
  public SqlArrayValue(String typeName, Object... elements) {
    Assert.notNull(typeName, "Type name is required");
    Assert.notNull(elements, "Elements array is required");
    this.typeName = typeName;
    this.elements = elements;
  }

  @Override
  public void setValue(PreparedStatement ps, int paramIndex) throws SQLException {
    this.array = ps.getConnection().createArrayOf(this.typeName, this.elements);
    ps.setArray(paramIndex, this.array);
  }

  @Override
  public void cleanup() {
    if (this.array != null) {
      try {
        this.array.free();
      }
      catch (SQLException ex) {
        throw new DataAccessResourceFailureException("Could not free Array object", ex);
      }
    }
  }

}
