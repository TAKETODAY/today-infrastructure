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

/**
 * The Derby specific implementation of {@link TableMetaDataProvider}.
 * Overrides the Derby meta-data info regarding retrieving generated keys.
 *
 * @author Thomas Risberg
 * @since 4.0
 */
public class DerbyTableMetaDataProvider extends GenericTableMetaDataProvider {

  private boolean supportsGeneratedKeysOverride = false;

  public DerbyTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
    super(databaseMetaData);
  }

  @Override
  public void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException {
    super.initializeWithMetaData(databaseMetaData);
    if (!databaseMetaData.supportsGetGeneratedKeys()) {
      if (logger.isInfoEnabled()) {
        logger.info("Overriding supportsGetGeneratedKeys from DatabaseMetaData to 'true'; it was reported as " +
                "'false' by " + databaseMetaData.getDriverName() + " " + databaseMetaData.getDriverVersion());
      }
      this.supportsGeneratedKeysOverride = true;
    }
  }

  @Override
  public boolean isGetGeneratedKeysSupported() {
    return (super.isGetGeneratedKeysSupported() || this.supportsGeneratedKeysOverride);
  }

}
