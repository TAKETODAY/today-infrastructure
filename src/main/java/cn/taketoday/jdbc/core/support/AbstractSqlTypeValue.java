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

package cn.taketoday.jdbc.core.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import cn.taketoday.jdbc.core.SqlTypeValue;
import cn.taketoday.lang.Nullable;

/**
 * Abstract implementation of the SqlTypeValue interface, for convenient
 * creation of type values that are supposed to be passed into the
 * {@code PreparedStatement.setObject} method. The {@code createTypeValue}
 * callback method has access to the underlying Connection, if that should
 * be needed to create any database-specific objects.
 *
 * <p>A usage example from a StoredProcedure (compare this to the plain
 * SqlTypeValue version in the superclass javadoc):
 *
 * <pre class="code">proc.declareParameter(new SqlParameter("myarray", Types.ARRAY, "NUMBERS"));
 * ...
 *
 * Map&lt;String, Object&gt; in = new HashMap&lt;String, Object&gt;();
 * in.put("myarray", new AbstractSqlTypeValue() {
 *   public Object createTypeValue(Connection con, int sqlType, String typeName) throws SQLException {
 * 	   oracle.sql.ArrayDescriptor desc = new oracle.sql.ArrayDescriptor(typeName, con);
 * 	   return new oracle.sql.ARRAY(desc, con, seats);
 *   }
 * });
 * Map out = execute(in);
 * </pre>
 *
 * @author Juergen Hoeller
 * @see PreparedStatement#setObject(int, Object, int)
 * @see cn.taketoday.jdbc.object.StoredProcedure
 * @since 4.0
 */
public abstract class AbstractSqlTypeValue implements SqlTypeValue {

  @Override
  public final void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
          throws SQLException {

    Object value = createTypeValue(ps.getConnection(), sqlType, typeName);
    if (sqlType == TYPE_UNKNOWN) {
      ps.setObject(paramIndex, value);
    }
    else {
      ps.setObject(paramIndex, value, sqlType);
    }
  }

  /**
   * Create the type value to be passed into {@code PreparedStatement.setObject}.
   *
   * @param con the JDBC Connection, if needed to create any database-specific objects
   * @param sqlType the SQL type of the parameter we are setting
   * @param typeName the type name of the parameter
   * @return the type value
   * @throws SQLException if an SQLException is encountered setting
   * parameter values (that is, there's no need to catch SQLException)
   * @see PreparedStatement#setObject(int, Object, int)
   */
  protected abstract Object createTypeValue(Connection con, int sqlType, @Nullable String typeName)
          throws SQLException;

}
