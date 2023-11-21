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

package cn.taketoday.jdbc.support;

import java.sql.DatabaseMetaData;
import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
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
  private static final Logger log = LoggerFactory.getLogger(SQLErrorCodesFactory.class);

  /**
   * The name of custom SQL error codes file, loading from the root
   * of the class path (e.g. from the "/WEB-INF/classes" directory).
   */
  public static final String SQL_ERROR_CODE_OVERRIDE_PATH = "sql-error-codes.xml";

  /**
   * The name of default SQL error code files, loading from the class path.
   */
  public static final String SQL_ERROR_CODE_DEFAULT_PATH = "cn/taketoday/jdbc/support/sql-error-codes.xml";

  /**
   * Keep track of a single instance so we can return it to classes that request it.
   */
  private static final SQLErrorCodesFactory instance = new SQLErrorCodesFactory();

  /**
   * Return the singleton instance.
   */
  public static SQLErrorCodesFactory of() {
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
  private final ConcurrentReferenceHashMap<DataSource, SQLErrorCodes> dataSourceCache = new ConcurrentReferenceHashMap<>(16);

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
    Map<String, SQLErrorCodes> errorCodes;

    try {
      StandardBeanFactory lbf = new StandardBeanFactory();
      lbf.setBeanClassLoader(getClass().getClassLoader());
      XmlBeanDefinitionReader bdr = new XmlBeanDefinitionReader(lbf);

      // Load default SQL error codes.
      Resource resource = loadResource(SQL_ERROR_CODE_DEFAULT_PATH);
      if (resource != null && resource.exists()) {
        bdr.loadBeanDefinitions(resource);
      }
      else {
        log.info("Default sql-error-codes.xml not found (should be included in today-jdbc jar)");
      }

      // Load custom SQL error codes, overriding defaults.
      resource = loadResource(SQL_ERROR_CODE_OVERRIDE_PATH);
      if (resource != null && resource.exists()) {
        bdr.loadBeanDefinitions(resource);
        log.debug("Found custom sql-error-codes.xml file at the root of the classpath");
      }

      // Check all beans of type SQLErrorCodes.
      errorCodes = lbf.getBeansOfType(SQLErrorCodes.class, true, false);
      if (log.isTraceEnabled()) {
        log.trace("SQLErrorCodes loaded: {}", errorCodes.keySet());
      }
    }
    catch (BeansException ex) {
      log.warn("Error loading SQL error codes from config file", ex);
      errorCodes = Collections.emptyMap();
    }

    this.errorCodesMap = errorCodes;
  }

  /**
   * Load the given resource from the class path.
   * <p><b>Not to be overridden by application developers, who should obtain
   * instances of this class from the static {@link #of()} method.</b>
   * <p>Protected for testability.
   *
   * @param path resource path; either a custom path or one of either
   * {@link #SQL_ERROR_CODE_DEFAULT_PATH} or
   * {@link #SQL_ERROR_CODE_OVERRIDE_PATH}.
   * @return the resource, or {@code null} if the resource wasn't found
   * @see #of
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
    Assert.notNull(databaseName, "Database product name is required");

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
      if (log.isDebugEnabled()) {
        log.debug("SQL error codes for '{}' found", databaseName);
      }
      return sec;
    }

    // Could not find the database among the defined ones.
    if (log.isDebugEnabled()) {
      log.debug("SQL error codes for '{}' not found", databaseName);
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
    Assert.notNull(dataSource, "DataSource is required");
    boolean debugEnabled = log.isDebugEnabled();
    if (debugEnabled) {
      log.debug("Looking up default SQLErrorCodes for DataSource [{}]", identify(dataSource));
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
            log.warn("Error while extracting database name", ex);
          }
          return null;
        }
      }
    }

    if (debugEnabled) {
      log.debug("SQLErrorCodes found in cache for DataSource [{}]", identify(dataSource));
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
    if (log.isDebugEnabled()) {
      log.debug("Caching SQL error codes for DataSource [{}]: database product name is '{}'",
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
      if (errorCodes.getCustomSqlExceptionTranslator() != null && log.isDebugEnabled()) {
        log.debug("Overriding already defined custom translator '{}' " +
                        "with '{}' found in the CustomSQLExceptionTranslatorRegistry for database '{}'",
                errorCodes.getCustomSqlExceptionTranslator().getClass().getSimpleName(),
                customTranslator.getClass().getSimpleName(), databaseName);
      }
      else if (log.isTraceEnabled()) {
        log.trace("Using custom translator '{}' found in the CustomSQLExceptionTranslatorRegistry for database '{}'",
                customTranslator.getClass().getSimpleName(), databaseName);
      }
      errorCodes.setCustomSqlExceptionTranslator(customTranslator);
    }
  }

}
