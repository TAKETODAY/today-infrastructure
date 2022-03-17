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

import java.lang.reflect.Constructor;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.DataSource;

import cn.taketoday.dao.CannotAcquireLockException;
import cn.taketoday.dao.CannotSerializeTransactionException;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.dao.DataIntegrityViolationException;
import cn.taketoday.dao.DeadlockLoserDataAccessException;
import cn.taketoday.dao.DuplicateKeyException;
import cn.taketoday.dao.PermissionDeniedDataAccessException;
import cn.taketoday.dao.TransientDataAccessResourceException;
import cn.taketoday.jdbc.BadSqlGrammarException;
import cn.taketoday.jdbc.InvalidResultSetAccessException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.SingletonSupplier;

/**
 * Implementation of {@link SQLExceptionTranslator} that analyzes vendor-specific error codes.
 * More precise than an implementation based on SQL state, but heavily vendor-specific.
 *
 * <p>This class applies the following matching rules:
 * <ul>
 * <li>Try custom translation implemented by any subclass. Note that this class is
 * concrete and is typically used itself, in which case this rule doesn't apply.
 * <li>Apply error code matching. Error codes are obtained from the SQLErrorCodesFactory
 * by default. This factory loads a "sql-error-codes.xml" file from the class path,
 * defining error code mappings for database names from database meta-data.
 * <li>Fallback to a fallback translator. {@link SQLStateSQLExceptionTranslator} is the
 * default fallback translator, analyzing the exception's SQL state only. On Java 6
 * which introduces its own {@code SQLException} subclass hierarchy, we will
 * use {@link SQLExceptionSubclassTranslator} by default, which in turns falls back
 * to Framework's own SQL state translation when not encountering specific subclasses.
 * </ul>
 *
 * <p>The configuration file named "sql-error-codes.xml" is by default read from
 * this package. It can be overridden through a file of the same name in the root
 * of the class path (e.g. in the "/WEB-INF/classes" directory), as long as the
 * Framework JDBC package is loaded from the same ClassLoader.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see SQLErrorCodesFactory
 * @see SQLStateSQLExceptionTranslator
 * @since 4.0
 */
public class SQLErrorCodeSQLExceptionTranslator extends AbstractFallbackSQLExceptionTranslator {

  private static final int MESSAGE_ONLY_CONSTRUCTOR = 1;
  private static final int MESSAGE_THROWABLE_CONSTRUCTOR = 2;
  private static final int MESSAGE_SQLEX_CONSTRUCTOR = 3;
  private static final int MESSAGE_SQL_THROWABLE_CONSTRUCTOR = 4;
  private static final int MESSAGE_SQL_SQLEX_CONSTRUCTOR = 5;

  /** Error codes used by this translator. */
  @Nullable
  private SingletonSupplier<SQLErrorCodes> sqlErrorCodes;

  /**
   * Constructor for use as a JavaBean.
   * The SqlErrorCodes or DataSource property must be set.
   */
  public SQLErrorCodeSQLExceptionTranslator() {
    setFallbackTranslator(new SQLExceptionSubclassTranslator());
  }

  /**
   * Create an SQL error code translator for the given DataSource.
   * Invoking this constructor will cause a Connection to be obtained
   * from the DataSource to get the meta-data.
   *
   * @param dataSource the DataSource to use to find meta-data and establish
   * which error codes are usable
   * @see SQLErrorCodesFactory
   */
  public SQLErrorCodeSQLExceptionTranslator(DataSource dataSource) {
    this();
    setDataSource(dataSource);
  }

  /**
   * Create an SQL error code translator for the given database product name.
   * Invoking this constructor will avoid obtaining a Connection from the
   * DataSource to get the meta-data.
   *
   * @param dbName the database product name that identifies the error codes entry
   * @see SQLErrorCodesFactory
   * @see java.sql.DatabaseMetaData#getDatabaseProductName()
   */
  public SQLErrorCodeSQLExceptionTranslator(String dbName) {
    this();
    setDatabaseProductName(dbName);
  }

  /**
   * Create an SQLErrorCode translator given these error codes.
   * Does not require a database meta-data lookup to be performed using a connection.
   *
   * @param sec error codes
   */
  public SQLErrorCodeSQLExceptionTranslator(SQLErrorCodes sec) {
    this();
    this.sqlErrorCodes = SingletonSupplier.valueOf(sec);
  }

  /**
   * Set the DataSource for this translator.
   * <p>Setting this property will cause a Connection to be obtained from
   * the DataSource to get the meta-data.
   *
   * @param dataSource the DataSource to use to find meta-data and establish
   * which error codes are usable
   * @see SQLErrorCodesFactory#getErrorCodes(javax.sql.DataSource)
   * @see java.sql.DatabaseMetaData#getDatabaseProductName()
   */
  public void setDataSource(DataSource dataSource) {
    this.sqlErrorCodes =
            SingletonSupplier.from(() -> SQLErrorCodesFactory.getInstance().resolveErrorCodes(dataSource));
    this.sqlErrorCodes.get();  // try early initialization - otherwise the supplier will retry later
  }

  /**
   * Set the database product name for this translator.
   * <p>Setting this property will avoid obtaining a Connection from the DataSource
   * to get the meta-data.
   *
   * @param dbName the database product name that identifies the error codes entry
   * @see SQLErrorCodesFactory#getErrorCodes(String)
   * @see java.sql.DatabaseMetaData#getDatabaseProductName()
   */
  public void setDatabaseProductName(String dbName) {
    this.sqlErrorCodes = SingletonSupplier.valueOf(SQLErrorCodesFactory.getInstance().getErrorCodes(dbName));
  }

  /**
   * Set custom error codes to be used for translation.
   *
   * @param sec custom error codes to use
   */
  public void setSqlErrorCodes(@Nullable SQLErrorCodes sec) {
    this.sqlErrorCodes = SingletonSupplier.ofNullable(sec);
  }

  /**
   * Return the error codes used by this translator.
   * Usually determined via a DataSource.
   *
   * @see #setDataSource
   */
  @Nullable
  public SQLErrorCodes getSqlErrorCodes() {
    return sqlErrorCodes != null ? sqlErrorCodes.get() : null;
  }

  @Override
  @Nullable
  protected DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex) {
    SQLException sqlEx = ex;
    if (sqlEx instanceof BatchUpdateException && sqlEx.getNextException() != null) {
      SQLException nestedSqlEx = sqlEx.getNextException();
      if (nestedSqlEx.getErrorCode() > 0 || nestedSqlEx.getSQLState() != null) {
        sqlEx = nestedSqlEx;
      }
    }

    // First, try custom translation from overridden method.
    DataAccessException dae = customTranslate(task, sql, sqlEx);
    if (dae != null) {
      return dae;
    }

    // Next, try the custom SQLException translator, if available.
    SQLErrorCodes sqlErrorCodes = getSqlErrorCodes();
    if (sqlErrorCodes != null) {
      SQLExceptionTranslator customTranslator = sqlErrorCodes.getCustomSqlExceptionTranslator();
      if (customTranslator != null) {
        DataAccessException customDex = customTranslator.translate(task, sql, sqlEx);
        if (customDex != null) {
          return customDex;
        }
      }
    }

    // Check SQLErrorCodes with corresponding error code, if available.
    if (sqlErrorCodes != null) {
      String errorCode;
      if (sqlErrorCodes.isUseSqlStateForTranslation()) {
        errorCode = sqlEx.getSQLState();
      }
      else {
        // Try to find SQLException with actual error code, looping through the causes.
        // E.g. applicable to java.sql.DataTruncation as of JDK 1.6.
        SQLException current = sqlEx;
        while (current.getErrorCode() == 0 && current.getCause() instanceof SQLException) {
          current = (SQLException) current.getCause();
        }
        errorCode = Integer.toString(current.getErrorCode());
      }

      if (errorCode != null) {
        // Look for defined custom translations first.
        CustomSQLErrorCodesTranslation[] customTranslations = sqlErrorCodes.getCustomTranslations();
        if (customTranslations != null) {
          for (CustomSQLErrorCodesTranslation customTranslation : customTranslations) {
            if (Arrays.binarySearch(customTranslation.getErrorCodes(), errorCode) >= 0
                    && customTranslation.getExceptionClass() != null) {
              DataAccessException customException = createCustomException(
                      task, sql, sqlEx, customTranslation.getExceptionClass());
              if (customException != null) {
                logTranslation(task, sql, sqlEx, true);
                return customException;
              }
            }
          }
        }
        // Next, look for grouped error codes.
        if (Arrays.binarySearch(sqlErrorCodes.getBadSqlGrammarCodes(), errorCode) >= 0) {
          logTranslation(task, sql, sqlEx, false);
          return new BadSqlGrammarException(task, (sql != null ? sql : ""), sqlEx);
        }
        else if (Arrays.binarySearch(sqlErrorCodes.getInvalidResultSetAccessCodes(), errorCode) >= 0) {
          logTranslation(task, sql, sqlEx, false);
          return new InvalidResultSetAccessException(task, (sql != null ? sql : ""), sqlEx);
        }
        else if (Arrays.binarySearch(sqlErrorCodes.getDuplicateKeyCodes(), errorCode) >= 0) {
          logTranslation(task, sql, sqlEx, false);
          return new DuplicateKeyException(buildMessage(task, sql, sqlEx), sqlEx);
        }
        else if (Arrays.binarySearch(sqlErrorCodes.getDataIntegrityViolationCodes(), errorCode) >= 0) {
          logTranslation(task, sql, sqlEx, false);
          return new DataIntegrityViolationException(buildMessage(task, sql, sqlEx), sqlEx);
        }
        else if (Arrays.binarySearch(sqlErrorCodes.getPermissionDeniedCodes(), errorCode) >= 0) {
          logTranslation(task, sql, sqlEx, false);
          return new PermissionDeniedDataAccessException(buildMessage(task, sql, sqlEx), sqlEx);
        }
        else if (Arrays.binarySearch(sqlErrorCodes.getDataAccessResourceFailureCodes(), errorCode) >= 0) {
          logTranslation(task, sql, sqlEx, false);
          return new DataAccessResourceFailureException(buildMessage(task, sql, sqlEx), sqlEx);
        }
        else if (Arrays.binarySearch(sqlErrorCodes.getTransientDataAccessResourceCodes(), errorCode) >= 0) {
          logTranslation(task, sql, sqlEx, false);
          return new TransientDataAccessResourceException(buildMessage(task, sql, sqlEx), sqlEx);
        }
        else if (Arrays.binarySearch(sqlErrorCodes.getCannotAcquireLockCodes(), errorCode) >= 0) {
          logTranslation(task, sql, sqlEx, false);
          return new CannotAcquireLockException(buildMessage(task, sql, sqlEx), sqlEx);
        }
        else if (Arrays.binarySearch(sqlErrorCodes.getDeadlockLoserCodes(), errorCode) >= 0) {
          logTranslation(task, sql, sqlEx, false);
          return new DeadlockLoserDataAccessException(buildMessage(task, sql, sqlEx), sqlEx);
        }
        else if (Arrays.binarySearch(sqlErrorCodes.getCannotSerializeTransactionCodes(), errorCode) >= 0) {
          logTranslation(task, sql, sqlEx, false);
          return new CannotSerializeTransactionException(buildMessage(task, sql, sqlEx), sqlEx);
        }
      }
    }

    // We couldn't identify it more precisely - let's hand it over to the SQLState fallback translator.
    if (logger.isDebugEnabled()) {
      String codes;
      if (sqlErrorCodes != null && sqlErrorCodes.isUseSqlStateForTranslation()) {
        codes = "SQL state '" + sqlEx.getSQLState() + "', error code '" + sqlEx.getErrorCode();
      }
      else {
        codes = "Error code '" + sqlEx.getErrorCode() + "'";
      }
      logger.debug("Unable to translate SQLException with {}, will now try the fallback translator", codes);
    }

    return null;
  }

  /**
   * Subclasses can override this method to attempt a custom mapping from
   * {@link SQLException} to {@link DataAccessException}.
   *
   * @param task readable text describing the task being attempted
   * @param sql the SQL query or update that caused the problem (may be {@code null})
   * @param sqlEx the offending SQLException
   * @return {@code null} if no custom translation applies, otherwise a {@link DataAccessException}
   * resulting from custom translation. This exception should include the {@code sqlEx} parameter
   * as a nested root cause. This implementation always returns {@code null}, meaning that the
   * translator always falls back to the default error codes.
   */
  @Nullable
  protected DataAccessException customTranslate(String task, @Nullable String sql, SQLException sqlEx) {
    return null;
  }

  /**
   * Create a custom {@link DataAccessException}, based on a given exception
   * class from a {@link CustomSQLErrorCodesTranslation} definition.
   *
   * @param task readable text describing the task being attempted
   * @param sql the SQL query or update that caused the problem (may be {@code null})
   * @param sqlEx the offending SQLException
   * @param exceptionClass the exception class to use, as defined in the
   * {@link CustomSQLErrorCodesTranslation} definition
   * @return {@code null} if the custom exception could not be created, otherwise
   * the resulting {@link DataAccessException}. This exception should include the
   * {@code sqlEx} parameter as a nested root cause.
   * @see CustomSQLErrorCodesTranslation#setExceptionClass
   */
  @Nullable
  protected DataAccessException createCustomException(
          String task, @Nullable String sql, SQLException sqlEx, Class<?> exceptionClass) {

    // Find appropriate constructor for the given exception class
    try {
      int constructorType = 0;
      Constructor<?>[] constructors = exceptionClass.getConstructors();
      for (Constructor<?> constructor : constructors) {
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        if (parameterTypes.length == 1
                && String.class == parameterTypes[0]
                && constructorType < MESSAGE_ONLY_CONSTRUCTOR) {
          constructorType = MESSAGE_ONLY_CONSTRUCTOR;
        }
        if (parameterTypes.length == 2
                && String.class == parameterTypes[0]
                && Throwable.class == parameterTypes[1]
                && constructorType < MESSAGE_THROWABLE_CONSTRUCTOR) {
          constructorType = MESSAGE_THROWABLE_CONSTRUCTOR;
        }
        if (parameterTypes.length == 2
                && String.class == parameterTypes[0]
                && SQLException.class == parameterTypes[1]
                && constructorType < MESSAGE_SQLEX_CONSTRUCTOR) {
          constructorType = MESSAGE_SQLEX_CONSTRUCTOR;
        }
        if (parameterTypes.length == 3
                && String.class == parameterTypes[0]
                && String.class == parameterTypes[1]
                && Throwable.class == parameterTypes[2]
                && constructorType < MESSAGE_SQL_THROWABLE_CONSTRUCTOR) {
          constructorType = MESSAGE_SQL_THROWABLE_CONSTRUCTOR;
        }
        if (parameterTypes.length == 3
                && String.class == parameterTypes[0]
                && String.class == parameterTypes[1]
                && SQLException.class == parameterTypes[2]
                && constructorType < MESSAGE_SQL_SQLEX_CONSTRUCTOR) {
          constructorType = MESSAGE_SQL_SQLEX_CONSTRUCTOR;
        }
      }

      // invoke constructor
      Constructor<?> exceptionConstructor;
      switch (constructorType) {
        case MESSAGE_SQL_SQLEX_CONSTRUCTOR -> {
          Class<?>[] messageAndSqlAndSqlExArgsClass = new Class<?>[] { String.class, String.class, SQLException.class };
          Object[] messageAndSqlAndSqlExArgs = new Object[] { task, sql, sqlEx };
          exceptionConstructor = exceptionClass.getConstructor(messageAndSqlAndSqlExArgsClass);
          return (DataAccessException) exceptionConstructor.newInstance(messageAndSqlAndSqlExArgs);
        }
        case MESSAGE_SQL_THROWABLE_CONSTRUCTOR -> {
          Class<?>[] messageAndSqlAndThrowableArgsClass = new Class<?>[] { String.class, String.class, Throwable.class };
          Object[] messageAndSqlAndThrowableArgs = new Object[] { task, sql, sqlEx };
          exceptionConstructor = exceptionClass.getConstructor(messageAndSqlAndThrowableArgsClass);
          return (DataAccessException) exceptionConstructor.newInstance(messageAndSqlAndThrowableArgs);
        }
        case MESSAGE_SQLEX_CONSTRUCTOR -> {
          Class<?>[] messageAndSqlExArgsClass = new Class<?>[] { String.class, SQLException.class };
          Object[] messageAndSqlExArgs = new Object[] { task + ": " + sqlEx.getMessage(), sqlEx };
          exceptionConstructor = exceptionClass.getConstructor(messageAndSqlExArgsClass);
          return (DataAccessException) exceptionConstructor.newInstance(messageAndSqlExArgs);
        }
        case MESSAGE_THROWABLE_CONSTRUCTOR -> {
          Class<?>[] messageAndThrowableArgsClass = new Class<?>[] { String.class, Throwable.class };
          Object[] messageAndThrowableArgs = new Object[] { task + ": " + sqlEx.getMessage(), sqlEx };
          exceptionConstructor = exceptionClass.getConstructor(messageAndThrowableArgsClass);
          return (DataAccessException) exceptionConstructor.newInstance(messageAndThrowableArgs);
        }
        case MESSAGE_ONLY_CONSTRUCTOR -> {
          Class<?>[] messageOnlyArgsClass = new Class<?>[] { String.class };
          Object[] messageOnlyArgs = new Object[] { task + ": " + sqlEx.getMessage() };
          exceptionConstructor = exceptionClass.getConstructor(messageOnlyArgsClass);
          return (DataAccessException) exceptionConstructor.newInstance(messageOnlyArgs);
        }
        default -> {
          if (logger.isWarnEnabled()) {
            logger.warn("Unable to find appropriate constructor of custom exception class [{}]", exceptionClass.getName());
          }
          return null;
        }
      }
    }
    catch (Throwable ex) {
      if (logger.isWarnEnabled()) {
        logger.warn("Unable to instantiate custom exception class [{}]", exceptionClass.getName(), ex);
      }
      return null;
    }
  }

  private void logTranslation(String task, @Nullable String sql, SQLException sqlEx, boolean custom) {
    if (logger.isDebugEnabled()) {
      String intro = custom ? "Custom translation of" : "Translating";
      logger.debug("{} SQLException with SQL state '{}', error code '{}', message [{}]{} for task [{}]",
              intro, sqlEx.getSQLState(), sqlEx.getErrorCode(),
              sqlEx.getMessage(), (sql != null ? "; SQL was [" + sql + "]" : ""), task);
    }
  }

}
