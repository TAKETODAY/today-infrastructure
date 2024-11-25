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

package infra.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import infra.jdbc.core.ColumnMapRowMapper;
import infra.jdbc.core.SqlOutParameter;
import infra.jdbc.core.SqlParameter;
import infra.lang.Nullable;

/**
 * Oracle-specific implementation for the {@link CallMetaDataProvider} interface.
 * This class is intended for internal use by the Simple JDBC classes.
 *
 * @author Thomas Risberg
 * @since 4.0
 */
public class OracleCallMetaDataProvider extends GenericCallMetaDataProvider {

  private static final String REF_CURSOR_NAME = "REF CURSOR";

  public OracleCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
    super(databaseMetaData);
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
    return -10;
  }

  @Override
  @Nullable
  public String metaDataCatalogNameToUse(@Nullable String catalogName) {
    // Oracle uses catalog name for package name or an empty string if no package
    return (catalogName == null ? "" : catalogNameToUse(catalogName));
  }

  @Override
  @Nullable
  public String metaDataSchemaNameToUse(@Nullable String schemaName) {
    // Use current user schema if no schema specified
    return (schemaName == null ? getUserName() : super.metaDataSchemaNameToUse(schemaName));
  }

  @Override
  public SqlParameter createDefaultOutParameter(String parameterName, CallParameterMetaData meta) {
    if (meta.getSqlType() == Types.OTHER && REF_CURSOR_NAME.equals(meta.getTypeName())) {
      return new SqlOutParameter(parameterName, getRefCursorSqlType(), new ColumnMapRowMapper());
    }
    else {
      return super.createDefaultOutParameter(parameterName, meta);
    }
  }

}