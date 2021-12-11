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

import javax.sql.DataSource;

import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.jdbc.support.MetaDataAccessException;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Factory used to create a {@link TableMetaDataProvider} implementation
 * based on the type of database being used.
 *
 * @author Thomas Risberg
 * @since 4.0
 */
public final class TableMetaDataProviderFactory {

  private static final Logger logger = LoggerFactory.getLogger(TableMetaDataProviderFactory.class);

  private TableMetaDataProviderFactory() {
  }

  /**
   * Create a {@link TableMetaDataProvider} based on the database meta-data.
   *
   * @param dataSource used to retrieve meta-data
   * @param context the class that holds configuration and meta-data
   * @return instance of the TableMetaDataProvider implementation to be used
   */
  public static TableMetaDataProvider createMetaDataProvider(DataSource dataSource, TableMetaDataContext context) {
    try {
      return JdbcUtils.extractDatabaseMetaData(dataSource, databaseMetaData -> {
        String databaseProductName = JdbcUtils.commonDatabaseName(databaseMetaData.getDatabaseProductName());
        TableMetaDataProvider provider;

        if ("Oracle".equals(databaseProductName)) {
          provider = new OracleTableMetaDataProvider(
                  databaseMetaData, context.isOverrideIncludeSynonymsDefault());
        }
        else if ("PostgreSQL".equals(databaseProductName)) {
          provider = new PostgresTableMetaDataProvider(databaseMetaData);
        }
        else if ("Apache Derby".equals(databaseProductName)) {
          provider = new DerbyTableMetaDataProvider(databaseMetaData);
        }
        else if ("HSQL Database Engine".equals(databaseProductName)) {
          provider = new HsqlTableMetaDataProvider(databaseMetaData);
        }
        else {
          provider = new GenericTableMetaDataProvider(databaseMetaData);
        }
        if (logger.isDebugEnabled()) {
          logger.debug("Using " + provider.getClass().getSimpleName());
        }

        provider.initializeWithMetaData(databaseMetaData);

        if (context.isAccessTableColumnMetaData()) {
          provider.initializeWithTableColumnMetaData(databaseMetaData,
                  context.getCatalogName(), context.getSchemaName(), context.getTableName());
        }

        return provider;
      });
    }
    catch (MetaDataAccessException ex) {
      throw new DataAccessResourceFailureException("Error retrieving database meta-data", ex);
    }
  }

}
