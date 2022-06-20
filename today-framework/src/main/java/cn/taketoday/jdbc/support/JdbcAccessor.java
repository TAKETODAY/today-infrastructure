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

import javax.sql.DataSource;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Base class for {@link cn.taketoday.jdbc.core.JdbcTemplate} and
 * other JDBC-accessing DAO helpers, defining common properties such as
 * DataSource and exception translator.
 *
 * <p>Not intended to be used directly.
 * See {@link cn.taketoday.jdbc.core.JdbcTemplate}.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @see cn.taketoday.jdbc.core.JdbcTemplate
 * @since 4.0
 */
public abstract class JdbcAccessor implements InitializingBean {

  @Nullable
  private DataSource dataSource;

  @Nullable
  private volatile SQLExceptionTranslator exceptionTranslator;

  private boolean lazyInit = true;

  /**
   * Set the JDBC DataSource to obtain connections from.
   */
  public void setDataSource(@Nullable DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * Return the DataSource used by this template.
   */
  @Nullable
  public DataSource getDataSource() {
    return this.dataSource;
  }

  /**
   * Obtain the DataSource for actual use.
   *
   * @return the DataSource (never {@code null})
   * @throws IllegalStateException in case of no DataSource set
   */
  protected DataSource obtainDataSource() {
    DataSource dataSource = getDataSource();
    Assert.state(dataSource != null, "No DataSource set");
    return dataSource;
  }

  /**
   * Specify the database product name for the DataSource that this accessor uses.
   * This allows to initialize an SQLErrorCodeSQLExceptionTranslator without
   * obtaining a Connection from the DataSource to get the meta-data.
   *
   * @param dbName the database product name that identifies the error codes entry
   * @see SQLErrorCodeSQLExceptionTranslator#setDatabaseProductName
   * @see java.sql.DatabaseMetaData#getDatabaseProductName()
   */
  public void setDatabaseProductName(String dbName) {
    if (SQLErrorCodeSQLExceptionTranslator.hasUserProvidedErrorCodesFile()) {
      this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dbName);
    }
    else {
      this.exceptionTranslator = new SQLExceptionSubclassTranslator();
    }
  }

  /**
   * Set the exception translator for this instance.
   * <p>If no custom translator is provided, a default
   * {@link SQLErrorCodeSQLExceptionTranslator} is used
   * which examines the SQLException's vendor-specific error code.
   *
   * @see cn.taketoday.jdbc.support.SQLErrorCodeSQLExceptionTranslator
   * @see cn.taketoday.jdbc.support.SQLStateSQLExceptionTranslator
   */
  public void setExceptionTranslator(SQLExceptionTranslator exceptionTranslator) {
    this.exceptionTranslator = exceptionTranslator;
  }

  /**
   * Return the exception translator for this instance.
   * <p>Creates a default {@link SQLErrorCodeSQLExceptionTranslator}
   * for the specified DataSource if none set, or a
   * {@link SQLStateSQLExceptionTranslator} in case of no DataSource.
   *
   * @see #getDataSource()
   */
  public SQLExceptionTranslator getExceptionTranslator() {
    SQLExceptionTranslator exceptionTranslator = this.exceptionTranslator;
    if (exceptionTranslator != null) {
      return exceptionTranslator;
    }
    synchronized(this) {
      exceptionTranslator = this.exceptionTranslator;
      if (exceptionTranslator == null) {
        if (SQLErrorCodeSQLExceptionTranslator.hasUserProvidedErrorCodesFile()) {
          exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(obtainDataSource());
        }
        else {
          exceptionTranslator = new SQLExceptionSubclassTranslator();
        }
        this.exceptionTranslator = exceptionTranslator;
      }
      return exceptionTranslator;
    }
  }

  /**
   * Set whether to lazily initialize the SQLExceptionTranslator for this accessor,
   * on first encounter of an SQLException. Default is "true"; can be switched to
   * "false" for initialization on startup.
   * <p>Early initialization just applies if {@code afterPropertiesSet()} is called.
   *
   * @see #getExceptionTranslator()
   * @see #afterPropertiesSet()
   */
  public void setLazyInit(boolean lazyInit) {
    this.lazyInit = lazyInit;
  }

  /**
   * Return whether to lazily initialize the SQLExceptionTranslator for this accessor.
   *
   * @see #getExceptionTranslator()
   */
  public boolean isLazyInit() {
    return this.lazyInit;
  }

  /**
   * Eagerly initialize the exception translator, if demanded,
   * creating a default one for the specified DataSource if none set.
   */
  @Override
  public void afterPropertiesSet() {
    if (getDataSource() == null) {
      throw new IllegalArgumentException("Property 'dataSource' is required");
    }
    if (!isLazyInit()) {
      getExceptionTranslator();
    }
  }

}
