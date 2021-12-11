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
 * The HSQL specific implementation of {@link TableMetaDataProvider}.
 * Supports a feature for retrieving generated keys without the JDBC 3.0
 * {@code getGeneratedKeys} support.
 *
 * @author Thomas Risberg
 * @since 4.0
 */
public class HsqlTableMetaDataProvider extends GenericTableMetaDataProvider {

  public HsqlTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
    super(databaseMetaData);
  }

  @Override
  public boolean isGetGeneratedKeysSimulated() {
    return true;
  }

  @Override
  public String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName) {
    return "select max(identity()) from " + tableName;
  }

}
