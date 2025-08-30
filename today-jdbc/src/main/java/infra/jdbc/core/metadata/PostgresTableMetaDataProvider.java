/*
 * Copyright 2017 - 2025 the original author or authors.
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

import infra.lang.Nullable;

/**
 * The PostgreSQL specific implementation of {@link TableMetaDataProvider}.
 * Supports a feature for retrieving generated keys without the JDBC 3.0
 * {@code getGeneratedKeys} support. Also, it processes PostgreSQL-returned
 * catalog and schema names from {@code DatabaseMetaData} in the given case.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class PostgresTableMetaDataProvider extends GenericTableMetaDataProvider {

  public PostgresTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
    super(databaseMetaData);
  }

  @Nullable
  @Override
  public String metaDataCatalogNameToUse(@Nullable String catalogName) {
    return catalogName;
  }

  @Nullable
  @Override
  public String metaDataSchemaNameToUse(@Nullable String schemaName) {
    return schemaName != null ? schemaName : getDefaultSchema();
  }

  @Override
  public boolean isGetGeneratedKeysSimulated() {
    return true;
  }

  @Override
  public String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName) {
    return "RETURNING " + keyColumnName;
  }

}
