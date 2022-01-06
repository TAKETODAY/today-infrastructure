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

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.jdbc.support.MetaDataAccessException;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Factory used to create a {@link CallMetaDataProvider} implementation
 * based on the type of database being used.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 4.0
 */
public final class CallMetaDataProviderFactory {

  /** List of supported database products for procedure calls. */
  public static final List<String> supportedDatabaseProductsForProcedures = Arrays.asList(
          "Apache Derby",
          "DB2",
          "Informix Dynamic Server",
          "MariaDB",
          "Microsoft SQL Server",
          "MySQL",
          "Oracle",
          "PostgreSQL",
          "Sybase"
  );

  /** List of supported database products for function calls. */
  public static final List<String> supportedDatabaseProductsForFunctions = Arrays.asList(
          "MariaDB",
          "Microsoft SQL Server",
          "MySQL",
          "Oracle",
          "PostgreSQL"
  );

  private static final Logger logger = LoggerFactory.getLogger(CallMetaDataProviderFactory.class);

  private CallMetaDataProviderFactory() { }

  /**
   * Create a {@link CallMetaDataProvider} based on the database meta-data.
   *
   * @param dataSource the JDBC DataSource to use for retrieving meta-data
   * @param context the class that holds configuration and meta-data
   * @return instance of the CallMetaDataProvider implementation to be used
   */
  public static CallMetaDataProvider createMetaDataProvider(DataSource dataSource, final CallMetaDataContext context) {
    try {
      return JdbcUtils.extractDatabaseMetaData(dataSource, databaseMetaData -> {
        String databaseProductName = JdbcUtils.commonDatabaseName(databaseMetaData.getDatabaseProductName());
        boolean accessProcedureColumnMetaData = context.isAccessCallParameterMetaData();
        if (context.isFunction()) {
          if (!supportedDatabaseProductsForFunctions.contains(databaseProductName)) {
            if (logger.isInfoEnabled()) {
              logger.info("{} is not one of the databases fully supported for function calls -- supported are: {}",
                      databaseProductName, supportedDatabaseProductsForFunctions);
            }
            if (accessProcedureColumnMetaData) {
              logger.info("Metadata processing disabled - you must specify all parameters explicitly");
              accessProcedureColumnMetaData = false;
            }
          }
        }
        else {
          if (!supportedDatabaseProductsForProcedures.contains(databaseProductName)) {
            if (logger.isInfoEnabled()) {
              logger.info("{} is not one of the databases fully supported for procedure calls -- supported are: {}",
                      databaseProductName, supportedDatabaseProductsForProcedures);
            }
            if (accessProcedureColumnMetaData) {
              logger.info("Metadata processing disabled - you must specify all parameters explicitly");
              accessProcedureColumnMetaData = false;
            }
          }
        }

        CallMetaDataProvider provider;
        if ("Oracle".equals(databaseProductName)) {
          provider = new OracleCallMetaDataProvider(databaseMetaData);
        }
        else if ("PostgreSQL".equals(databaseProductName)) {
          provider = new PostgresCallMetaDataProvider((databaseMetaData));
        }
        else if ("Apache Derby".equals(databaseProductName)) {
          provider = new DerbyCallMetaDataProvider((databaseMetaData));
        }
        else if ("DB2".equals(databaseProductName)) {
          provider = new Db2CallMetaDataProvider((databaseMetaData));
        }
        else if ("HDB".equals(databaseProductName)) {
          provider = new HanaCallMetaDataProvider((databaseMetaData));
        }
        else if ("Microsoft SQL Server".equals(databaseProductName)) {
          provider = new SqlServerCallMetaDataProvider((databaseMetaData));
        }
        else if ("Sybase".equals(databaseProductName)) {
          provider = new SybaseCallMetaDataProvider((databaseMetaData));
        }
        else {
          provider = new GenericCallMetaDataProvider(databaseMetaData);
        }

        if (logger.isDebugEnabled()) {
          logger.debug("Using " + provider.getClass().getName());
        }
        provider.initializeWithMetaData(databaseMetaData);
        if (accessProcedureColumnMetaData) {
          provider.initializeWithProcedureColumnMetaData(databaseMetaData,
                  context.getCatalogName(), context.getSchemaName(), context.getProcedureName());
        }
        return provider;
      });
    }
    catch (MetaDataAccessException ex) {
      throw new DataAccessResourceFailureException("Error retrieving database meta-data", ex);
    }
  }

}
