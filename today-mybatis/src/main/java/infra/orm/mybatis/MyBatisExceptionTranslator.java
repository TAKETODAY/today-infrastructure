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

package infra.orm.mybatis;

import org.apache.ibatis.exceptions.PersistenceException;

import java.sql.SQLException;
import java.util.function.Supplier;

import javax.sql.DataSource;

import infra.dao.DataAccessException;
import infra.dao.support.PersistenceExceptionTranslator;
import infra.jdbc.UncategorizedSQLException;
import infra.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import infra.jdbc.support.SQLExceptionTranslator;
import infra.lang.Nullable;
import infra.transaction.TransactionException;

/**
 * Default exception translator.
 *
 * Translates MyBatis SqlSession returned exception into a {@code DataAccessException} using Framework's
 * {@code SQLExceptionTranslator} Can load {@code SQLExceptionTranslator} eagerly or when the first exception is
 * translated.
 *
 * @author Eduardo Macarron
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MyBatisExceptionTranslator implements PersistenceExceptionTranslator {

  @Nullable
  private SQLExceptionTranslator exceptionTranslator;

  private final Supplier<SQLExceptionTranslator> exceptionTranslatorSupplier;

  /**
   * Creates a new {@code PersistenceExceptionTranslator} instance with {@code SQLErrorCodeSQLExceptionTranslator}.
   *
   * @param dataSource DataSource to use to find metadata and establish which error codes are usable.
   * @param exceptionTranslatorLazyInit if true, the translator instantiates internal stuff only the first time will have the need to translate
   * exceptions.
   */
  public MyBatisExceptionTranslator(DataSource dataSource, boolean exceptionTranslatorLazyInit) {
    this(() -> new SQLErrorCodeSQLExceptionTranslator(dataSource), exceptionTranslatorLazyInit);
  }

  /**
   * Creates a new {@code PersistenceExceptionTranslator} instance with specified {@code SQLExceptionTranslator}.
   *
   * @param exceptionTranslatorSupplier Supplier for creating a {@code SQLExceptionTranslator} instance
   * @param exceptionTranslatorLazyInit if true, the translator instantiates internal stuff only the first time will have the need to translate
   * exceptions.
   */
  public MyBatisExceptionTranslator(Supplier<SQLExceptionTranslator> exceptionTranslatorSupplier,
          boolean exceptionTranslatorLazyInit) {
    this.exceptionTranslatorSupplier = exceptionTranslatorSupplier;
    if (!exceptionTranslatorLazyInit) {
      this.initExceptionTranslator();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DataAccessException translateExceptionIfPossible(RuntimeException e) {
    if (e instanceof PersistenceException) {
      // Batch exceptions come inside another PersistenceException
      // recursion has a risk of infinite loop so better make another if
      if (e.getCause() instanceof PersistenceException) {
        e = (PersistenceException) e.getCause();
      }
      if (e.getCause() instanceof SQLException se) {
        this.initExceptionTranslator();
        String task = e.getMessage() + "\n";
        DataAccessException dae = null;
        if (exceptionTranslator != null) {
          dae = exceptionTranslator.translate(task, null, se);
        }
        return dae != null ? dae : new UncategorizedSQLException(task, null, se);
      }
      else if (e.getCause() instanceof TransactionException) {
        throw (TransactionException) e.getCause();
      }
      return new MyBatisSystemException(e);
    }
    return null;
  }

  /**
   * Initializes the internal translator reference.
   */
  private synchronized void initExceptionTranslator() {
    if (this.exceptionTranslator == null) {
      this.exceptionTranslator = exceptionTranslatorSupplier.get();
    }
  }

}
