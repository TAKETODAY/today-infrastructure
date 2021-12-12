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

package cn.taketoday.jdbc.support;

import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.jdbc.utils.JdbcUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.StringUtils;

/**
 * Factory for creating {@link SQLErrorCodes} based on the
 * "databaseProductName" taken from the {@link java.sql.DatabaseMetaData}.
 *
 * <p>Returns {@code SQLErrorCodes} populated with vendor codes
 * defined in a configuration file named "sql-error-codes.xml".
 * Reads the default file in this package if not overridden by a file in
 * the root of the class path (for example in the "/WEB-INF/classes" directory).
 *
 * @author Thomas Risberg
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
 */
public class SQLErrorCodesFactory {

  /**
   * The name of custom SQL error codes file, loading from the root
   * of the class path (e.g. from the "/WEB-INF/classes" directory).
   */
  public static final String SQL_ERROR_CODE_OVERRIDE_PATH = "sql-error-codes.xml";

  /**
   * The name of default SQL error code files, loading from the class path.
   */
  public static final String SQL_ERROR_CODE_DEFAULT_PATH = "cn.taketoday/jdbc/support/sql-error-codes.xml";

  private static final Logger logger = LoggerFactory.getLogger(SQLErrorCodesFactory.class);

  /**
   * Keep track of a single instance so we can return it to classes that request it.
   */
  private static final SQLErrorCodesFactory instance = new SQLErrorCodesFactory();

  /**
   * Return the singleton instance.
   */
  public static SQLErrorCodesFactory getInstance() {
    return instance;
  }

  /**
   * Map to hold error codes for all databases defined in the config file.
   * Key is the database product name, value is the SQLErrorCodes instance.
   */
  private final Map<String, SQLErrorCodes> errorCodesMap;

  /**
   * Map to cache the SQLErrorCodes instance per DataSource.
   */
  private final Map<DataSource, SQLErrorCodes> dataSourceCache = new ConcurrentReferenceHashMap<>(16);

  /**
   * Create a new instance of the {@link SQLErrorCodesFactory} class.
   * <p>Not public to enforce Singleton design pattern. Would be private
   * except to allow testing via overriding the
   * {@link #loadResource(String)} method.
   * <p><b>Do not subclass in application code.</b>
   *
   * @see #loadResource(String)
   */
  protected SQLErrorCodesFactory() {
    Map<String, SQLErrorCodes> errorCodes = new HashMap<>();

    SQLErrorCodes db2ErrorCodes = new SQLErrorCodes();
    db2ErrorCodes.setDatabaseProductName("DB2*");
    db2ErrorCodes.setBadSqlGrammarCodes("-007,-029,-097,-104,-109,-115,-128,-199,-204,-206,-301,-408,-441,-491".split(","));
    db2ErrorCodes.setDuplicateKeyCodes("-803");
    db2ErrorCodes.setDataIntegrityViolationCodes("-407,-530,-531,-532,-543,-544,-545,-603,-667".split(","));
    db2ErrorCodes.setDataAccessResourceFailureCodes("-904", "-971");
    db2ErrorCodes.setTransientDataAccessResourceCodes("-1035", "-1218", "-30080", "-30081");
    db2ErrorCodes.setDeadlockLoserCodes("-911", "-913");
    errorCodes.put("DB2", db2ErrorCodes);
    errorCodes.put("Db2", db2ErrorCodes);

    // Derby
    SQLErrorCodes derbyCodes = new SQLErrorCodes();

    derbyCodes.setDatabaseProductName("Apache Derby");
    derbyCodes.setUseSqlStateForTranslation(true);
    derbyCodes.setBadSqlGrammarCodes("42802,42821,42X01,42X02,42X03,42X04,42X05,42X06,42X07,42X08".split(","));
    derbyCodes.setDuplicateKeyCodes("23505");
    derbyCodes.setDataIntegrityViolationCodes("22001,22005,23502,23503,23513,X0Y32".split(","));
    derbyCodes.setDataAccessResourceFailureCodes("04501", "08004", "42Y07");
    derbyCodes.setDeadlockLoserCodes("40001");
    derbyCodes.setCannotAcquireLockCodes("40XL1");
    errorCodes.put("Derby", derbyCodes);

    // H2
    SQLErrorCodes h2Codes = new SQLErrorCodes();

    h2Codes.setDatabaseProductName("Apache Derby");
    h2Codes.setBadSqlGrammarCodes("42000,42001,42101,42102,42111,42112,42121,42122,42132".split(","));
    h2Codes.setDuplicateKeyCodes("23001", "23505");
    h2Codes.setDataIntegrityViolationCodes("22001,22003,22012,22018,22025,23000,23002,23003,23502,23503,23506,23507,23513".split(","));
    h2Codes.setDataAccessResourceFailureCodes("90046,90100,90117,90121,90126".split(","));
    h2Codes.setCannotAcquireLockCodes("50200");
    errorCodes.put("H2", h2Codes);

    // MySQL
    SQLErrorCodes mySQLCodes = new SQLErrorCodes();
    mySQLCodes.setDatabaseProductNames("MySQL", "MariaDB");
    mySQLCodes.setBadSqlGrammarCodes("1054,1064,1146".split(","));
    mySQLCodes.setDuplicateKeyCodes("1062");
    mySQLCodes.setDataIntegrityViolationCodes("630,839,840,893,1169,1215,1216,1217,1364,1451,1452,1557".split(","));
    mySQLCodes.setDataAccessResourceFailureCodes("1");
    mySQLCodes.setCannotAcquireLockCodes("1205", "3572");
    mySQLCodes.setDeadlockLoserCodes("1213");
    errorCodes.put("MySQL", mySQLCodes);

    // Oracle
    SQLErrorCodes oracleCodes = new SQLErrorCodes();
    oracleCodes.setBadSqlGrammarCodes("900,903,904,917,936,942,17006,6550".split(","));
    oracleCodes.setInvalidResultSetAccessCodes("17003");
    oracleCodes.setDuplicateKeyCodes("1");
    oracleCodes.setDataIntegrityViolationCodes("1400,1722,2291,2292".split(","));
    oracleCodes.setDataAccessResourceFailureCodes("17002", "17447");
    oracleCodes.setCannotAcquireLockCodes("54", "30006");
    oracleCodes.setDeadlockLoserCodes("60");
    oracleCodes.setCannotSerializeTransactionCodes("8177");
    errorCodes.put("Oracle", oracleCodes);

    // Oracle
    SQLErrorCodes postgresCodes = new SQLErrorCodes();
    postgresCodes.setUseSqlStateForTranslation(true);
    postgresCodes.setBadSqlGrammarCodes("03000,42000,42601,42602,42622,42804,42P01".split(","));
    postgresCodes.setInvalidResultSetAccessCodes("17003");
    postgresCodes.setDuplicateKeyCodes("21000", "23505");
    postgresCodes.setDataIntegrityViolationCodes("23000,23502,23503,23514".split(","));
    postgresCodes.setDataAccessResourceFailureCodes("53000", "53100", "53200", "53300");
    postgresCodes.setCannotAcquireLockCodes("55P03");
    postgresCodes.setDeadlockLoserCodes("40P01");
    postgresCodes.setCannotSerializeTransactionCodes("40001");
    errorCodes.put("Postgres", postgresCodes);
    errorCodes.put("PostgreSQL", postgresCodes);

    // SqlServer
    SQLErrorCodes sqlServerCodes = new SQLErrorCodes();
    sqlServerCodes.setDatabaseProductName("Microsoft SQL Server");
    sqlServerCodes.setBadSqlGrammarCodes("156,170,207,208,209".split(","));
    sqlServerCodes.setPermissionDeniedCodes("229");
    sqlServerCodes.setDuplicateKeyCodes("2601", "2627");
    sqlServerCodes.setDataIntegrityViolationCodes("544", "8114", "8115");
    sqlServerCodes.setDataAccessResourceFailureCodes("4060");
    sqlServerCodes.setCannotAcquireLockCodes("1222");
    sqlServerCodes.setDeadlockLoserCodes("1205");

    errorCodes.put("MS-SQL", sqlServerCodes);
    errorCodes.put("SqlServer", sqlServerCodes);

    // HSQL
    SQLErrorCodes hSQLCodes = new SQLErrorCodes();
    hSQLCodes.setDatabaseProductName("HSQL Database Engine");
    hSQLCodes.setBadSqlGrammarCodes("-22", "-28");
    hSQLCodes.setDuplicateKeyCodes("-104");
    hSQLCodes.setDataIntegrityViolationCodes("-9");
    hSQLCodes.setDataAccessResourceFailureCodes("-80");
    errorCodes.put("Hsql", hSQLCodes);
    errorCodes.put("HSQL", hSQLCodes);

    // Sybase
    SQLErrorCodes sybaseCodes = new SQLErrorCodes();
    sybaseCodes.setDatabaseProductNames(
            "Sybase SQL Server", "Adaptive Server Enterprise", "ASE", "SQL Server", "sql server");
    sybaseCodes.setBadSqlGrammarCodes("101,102,103,104,105,106,107,108,109,110,111,112,113,116,120,121,123,207,208,213,257,512".split(","));
    sybaseCodes.setDuplicateKeyCodes("2601", "2615", "2626");
    sybaseCodes.setDataIntegrityViolationCodes("233", "511", "515", "530", "546", "547", "2615", "2714");
    sybaseCodes.setTransientDataAccessResourceCodes("921", "1105");
    sybaseCodes.setCannotAcquireLockCodes("12205");
    sybaseCodes.setDeadlockLoserCodes("1205");
    errorCodes.put("Sybase", sybaseCodes);

    // Informix
    SQLErrorCodes informixCodes = new SQLErrorCodes();
    informixCodes.setDatabaseProductName("Informix Dynamic Server");
    informixCodes.setBadSqlGrammarCodes("-201", "-217", "-696");
    informixCodes.setDuplicateKeyCodes("-239", "-268", "-6017");
    informixCodes.setDataIntegrityViolationCodes("-692", "-11030");
    errorCodes.put("Informix", informixCodes);

    // HDB
    SQLErrorCodes hDBCodes = new SQLErrorCodes();
    hDBCodes.setDatabaseProductNames("SAP HANA", "SAP DB");
    hDBCodes.setBadSqlGrammarCodes("""
            257,259,260,261,262,263,264,267,268,269,270,271,272,273,275,276,277,278,
            278,279,280,281,282,283,284,285,286,288,289,290,294,295,296,297,299,308,309,
            313,315,316,318,319,320,321,322,323,324,328,329,330,333,335,336,337,338,340,
            343,350,351,352,362,368F""".split(","));
    hDBCodes.setPermissionDeniedCodes("10", "258");
    hDBCodes.setDuplicateKeyCodes("301");
    hDBCodes.setDataIntegrityViolationCodes("461", "462");
    hDBCodes.setDataAccessResourceFailureCodes("-813,-709,-708,1024,1025,1026,1027,1029,1030,1031".split(","));
    hDBCodes.setInvalidResultSetAccessCodes("-11210", "582", "587", "588", "594");
    hDBCodes.setCannotAcquireLockCodes("131");
    hDBCodes.setCannotSerializeTransactionCodes("138", "143");
    hDBCodes.setDeadlockLoserCodes("133");
    errorCodes.put("HDB", hDBCodes);
    errorCodes.put("Hana", hDBCodes);

    this.errorCodesMap = errorCodes;
  }

  /**
   * Load the given resource from the class path.
   * <p><b>Not to be overridden by application developers, who should obtain
   * instances of this class from the static {@link #getInstance()} method.</b>
   * <p>Protected for testability.
   *
   * @param path resource path; either a custom path or one of either
   * {@link #SQL_ERROR_CODE_DEFAULT_PATH} or
   * {@link #SQL_ERROR_CODE_OVERRIDE_PATH}.
   * @return the resource, or {@code null} if the resource wasn't found
   * @see #getInstance
   */
  @Nullable
  protected Resource loadResource(String path) {
    return new ClassPathResource(path, getClass().getClassLoader());
  }

  /**
   * Return the {@link SQLErrorCodes} instance for the given database.
   * <p>No need for a database meta-data lookup.
   *
   * @param databaseName the database name (must not be {@code null})
   * @return the {@code SQLErrorCodes} instance for the given database
   * (never {@code null}; potentially empty)
   * @throws IllegalArgumentException if the supplied database name is {@code null}
   */
  public SQLErrorCodes getErrorCodes(String databaseName) {
    Assert.notNull(databaseName, "Database product name must not be null");

    SQLErrorCodes sec = this.errorCodesMap.get(databaseName);
    if (sec == null) {
      for (SQLErrorCodes candidate : this.errorCodesMap.values()) {
        if (StringUtils.simpleMatch(candidate.getDatabaseProductNames(), databaseName)) {
          sec = candidate;
          break;
        }
      }
    }
    if (sec != null) {
      checkCustomTranslatorRegistry(databaseName, sec);
      if (logger.isDebugEnabled()) {
        logger.debug("SQL error codes for '{}' found", databaseName);
      }
      return sec;
    }

    // Could not find the database among the defined ones.
    if (logger.isDebugEnabled()) {
      logger.debug("SQL error codes for '{}' not found", databaseName);
    }
    return new SQLErrorCodes();
  }

  /**
   * Return {@link SQLErrorCodes} for the given {@link DataSource},
   * evaluating "databaseProductName" from the
   * {@link java.sql.DatabaseMetaData}, or an empty error codes
   * instance if no {@code SQLErrorCodes} were found.
   *
   * @param dataSource the {@code DataSource} identifying the database
   * @return the corresponding {@code SQLErrorCodes} object
   * (never {@code null}; potentially empty)
   * @see java.sql.DatabaseMetaData#getDatabaseProductName()
   */
  public SQLErrorCodes getErrorCodes(DataSource dataSource) {
    SQLErrorCodes sec = resolveErrorCodes(dataSource);
    return (sec != null ? sec : new SQLErrorCodes());
  }

  /**
   * Return {@link SQLErrorCodes} for the given {@link DataSource},
   * evaluating "databaseProductName" from the
   * {@link java.sql.DatabaseMetaData}, or {@code null} if case
   * of a JDBC meta-data access problem.
   *
   * @param dataSource the {@code DataSource} identifying the database
   * @return the corresponding {@code SQLErrorCodes} object,
   * or {@code null} in case of a JDBC meta-data access problem
   * @see java.sql.DatabaseMetaData#getDatabaseProductName()
   */
  @Nullable
  public SQLErrorCodes resolveErrorCodes(DataSource dataSource) {
    Assert.notNull(dataSource, "DataSource must not be null");
    if (logger.isDebugEnabled()) {
      logger.debug("Looking up default SQLErrorCodes for DataSource [{}]", identify(dataSource));
    }

    // Try efficient lock-free access for existing cache entry
    SQLErrorCodes sec = this.dataSourceCache.get(dataSource);
    if (sec == null) {
      synchronized(this.dataSourceCache) {
        // Double-check within full dataSourceCache lock
        sec = this.dataSourceCache.get(dataSource);
        if (sec == null) {
          // We could not find it - got to look it up.
          try {
            String name = JdbcUtils.extractDatabaseMetaData(
                    dataSource, DatabaseMetaData::getDatabaseProductName);
            if (StringUtils.isNotEmpty(name)) {
              return registerDatabase(dataSource, name);
            }
          }
          catch (MetaDataAccessException ex) {
            logger.warn("Error while extracting database name", ex);
          }
          return null;
        }
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("SQLErrorCodes found in cache for DataSource [{}]", identify(dataSource));
    }

    return sec;
  }

  /**
   * Associate the specified database name with the given {@link DataSource}.
   *
   * @param dataSource the {@code DataSource} identifying the database
   * @param databaseName the corresponding database name as stated in the error codes
   * definition file (must not be {@code null})
   * @return the corresponding {@code SQLErrorCodes} object (never {@code null})
   * @see #unregisterDatabase(DataSource)
   */
  public SQLErrorCodes registerDatabase(DataSource dataSource, String databaseName) {
    SQLErrorCodes sec = getErrorCodes(databaseName);
    if (logger.isDebugEnabled()) {
      logger.debug("Caching SQL error codes for DataSource [{}]: database product name is '{}'",
              identify(dataSource), databaseName);
    }
    this.dataSourceCache.put(dataSource, sec);
    return sec;
  }

  /**
   * Clear the cache for the specified {@link DataSource}, if registered.
   *
   * @param dataSource the {@code DataSource} identifying the database
   * @return the corresponding {@code SQLErrorCodes} object that got removed,
   * or {@code null} if not registered
   * @see #registerDatabase(DataSource, String)
   */
  @Nullable
  public SQLErrorCodes unregisterDatabase(DataSource dataSource) {
    return this.dataSourceCache.remove(dataSource);
  }

  /**
   * Build an identification String for the given {@link DataSource},
   * primarily for logging purposes.
   *
   * @param dataSource the {@code DataSource} to introspect
   * @return the identification String
   */
  private String identify(DataSource dataSource) {
    return dataSource.getClass().getName() + '@' + Integer.toHexString(dataSource.hashCode());
  }

  /**
   * Check the {@link CustomSQLExceptionTranslatorRegistry} for any entries.
   */
  private void checkCustomTranslatorRegistry(String databaseName, SQLErrorCodes errorCodes) {
    SQLExceptionTranslator customTranslator =
            CustomSQLExceptionTranslatorRegistry.getInstance().findTranslatorForDatabase(databaseName);
    if (customTranslator != null) {
      if (errorCodes.getCustomSqlExceptionTranslator() != null && logger.isDebugEnabled()) {
        logger.debug("Overriding already defined custom translator '{}' " +
                        "with '{}' found in the CustomSQLExceptionTranslatorRegistry for database '{}'",
                errorCodes.getCustomSqlExceptionTranslator().getClass().getSimpleName(),
                customTranslator.getClass().getSimpleName(), databaseName);
      }
      else if (logger.isTraceEnabled()) {
        logger.trace("Using custom translator '{}' found in the CustomSQLExceptionTranslatorRegistry for database '{}'",
                customTranslator.getClass().getSimpleName(), databaseName);
      }
      errorCodes.setCustomSqlExceptionTranslator(customTranslator);
    }
  }

}
