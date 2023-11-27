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

package cn.taketoday.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import cn.taketoday.jdbc.core.ColumnMapRowMapper;
import cn.taketoday.jdbc.core.SqlOutParameter;
import cn.taketoday.jdbc.core.SqlParameter;
import cn.taketoday.lang.Nullable;

/**
 * Postgres-specific implementation for the {@link CallMetaDataProvider} interface.
 * This class is intended for internal use by the Simple JDBC classes.
 *
 * @author Thomas Risberg
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PostgresCallMetaDataProvider extends GenericCallMetaDataProvider {

  private static final String RETURN_VALUE_NAME = "returnValue";

  private final String schemaName;

  public PostgresCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
    super(databaseMetaData);

    // Use current schema (or public schema) if no schema specified
    String schema = databaseMetaData.getConnection().getSchema();
    this.schemaName = (schema != null ? schema : "public");
  }

  @Override
  public boolean isReturnResultSetSupported() {
    return false;
  }

  @Override
  public boolean isRefCursorSupported() {
    return true;
  }

  @Override
  public int getRefCursorSqlType() {
    return Types.OTHER;
  }

  @Override
  @Nullable
  public String metaDataSchemaNameToUse(@Nullable String schemaName) {
    return (schemaName == null ? this.schemaName : super.metaDataSchemaNameToUse(schemaName));
  }

  @Override
  public SqlParameter createDefaultOutParameter(String parameterName, CallParameterMetaData meta) {
    if (meta.getSqlType() == Types.OTHER && "refcursor".equals(meta.getTypeName())) {
      return new SqlOutParameter(parameterName, getRefCursorSqlType(), new ColumnMapRowMapper());
    }
    else {
      return super.createDefaultOutParameter(parameterName, meta);
    }
  }

  @Override
  public boolean byPassReturnParameter(String parameterName) {
    return RETURN_VALUE_NAME.equals(parameterName);
  }

}
