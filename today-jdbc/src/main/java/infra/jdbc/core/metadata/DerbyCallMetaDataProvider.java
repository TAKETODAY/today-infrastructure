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
import java.util.Locale;

import infra.lang.Nullable;

/**
 * Derby specific implementation for the {@link CallMetaDataProvider} interface.
 * This class is intended for internal use by the Simple JDBC classes.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DerbyCallMetaDataProvider extends GenericCallMetaDataProvider {

  public DerbyCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
    super(databaseMetaData);
  }

  @Override
  @Nullable
  public String metaDataSchemaNameToUse(@Nullable String schemaName) {
    if (schemaName != null) {
      return super.metaDataSchemaNameToUse(schemaName);
    }

    // Use current user schema if no schema specified...
    String userName = getUserName();
    return (userName != null ? userName.toUpperCase(Locale.ROOT) : null);
  }

}
