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

package cn.taketoday.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import cn.taketoday.lang.Nullable;

/**
 * DB2 specific implementation for the {@link CallMetaDataProvider} interface.
 * This class is intended for internal use by the Simple JDBC classes.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 4.0
 */
public class Db2CallMetaDataProvider extends GenericCallMetaDataProvider {

  public Db2CallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
    super(databaseMetaData);
  }

  @Override
  public void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException {
    try {
      setSupportsCatalogsInProcedureCalls(databaseMetaData.supportsCatalogsInProcedureCalls());
    }
    catch (SQLException ex) {
      logger.debug("Error retrieving 'DatabaseMetaData.supportsCatalogsInProcedureCalls' - {}", ex.getMessage());
    }
    try {
      setSupportsSchemasInProcedureCalls(databaseMetaData.supportsSchemasInProcedureCalls());
    }
    catch (SQLException ex) {
      logger.debug("Error retrieving 'DatabaseMetaData.supportsSchemasInProcedureCalls' - {}", ex.getMessage());
    }
    try {
      setStoresUpperCaseIdentifiers(databaseMetaData.storesUpperCaseIdentifiers());
    }
    catch (SQLException ex) {
      logger.debug("Error retrieving 'DatabaseMetaData.storesUpperCaseIdentifiers' - {}", ex.getMessage());
    }
    try {
      setStoresLowerCaseIdentifiers(databaseMetaData.storesLowerCaseIdentifiers());
    }
    catch (SQLException ex) {
      logger.debug("Error retrieving 'DatabaseMetaData.storesLowerCaseIdentifiers' - {}", ex.getMessage());
    }
  }

  @Override
  @Nullable
  public String metaDataSchemaNameToUse(@Nullable String schemaName) {
    if (schemaName != null) {
      return super.metaDataSchemaNameToUse(schemaName);
    }

    // Use current user schema if no schema specified...
    String userName = getUserName();
    return (userName != null ? userName.toUpperCase() : null);
  }

}
