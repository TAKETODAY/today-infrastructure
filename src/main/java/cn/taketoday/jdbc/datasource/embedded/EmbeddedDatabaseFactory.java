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

package cn.taketoday.jdbc.datasource.embedded;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import cn.taketoday.jdbc.datasource.SimpleDriverDataSource;
import cn.taketoday.jdbc.datasource.init.DatabasePopulator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Factory for creating an {@link EmbeddedDatabase} instance.
 *
 * <p>Callers are guaranteed that the returned database has been fully
 * initialized and populated.
 *
 * <p>The factory can be configured as follows:
 * <ul>
 * <li>Call {@link #generateUniqueDatabaseName} to set a unique, random name
 * for the database.
 * <li>Call {@link #setDatabaseName} to set an explicit name for the database.
 * <li>Call {@link #setDatabaseType} to set the database type if you wish to
 * use one of the supported types.
 * <li>Call {@link #setDatabaseConfigurer} to configure support for a custom
 * embedded database type.
 * <li>Call {@link #setDatabasePopulator} to change the algorithm used to
 * populate the database.
 * <li>Call {@link #setDataSourceFactory} to change the type of
 * {@link DataSource} used to connect to the database.
 * </ul>
 *
 * <p>After configuring the factory, call {@link #getDatabase()} to obtain
 * a reference to the {@link EmbeddedDatabase} instance.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
public class EmbeddedDatabaseFactory {

  /**
   * Default name for an embedded database: {@value}.
   */
  public static final String DEFAULT_DATABASE_NAME = "testdb";

  private static final Logger logger = LoggerFactory.getLogger(EmbeddedDatabaseFactory.class);

  private boolean generateUniqueDatabaseName = false;

  private String databaseName = DEFAULT_DATABASE_NAME;

  private DataSourceFactory dataSourceFactory = new SimpleDriverDataSourceFactory();

  @Nullable
  private EmbeddedDatabaseConfigurer databaseConfigurer;

  @Nullable
  private DatabasePopulator databasePopulator;

  @Nullable
  private DataSource dataSource;

  /**
   * Set the {@code generateUniqueDatabaseName} flag to enable or disable
   * generation of a pseudo-random unique ID to be used as the database name.
   * <p>Setting this flag to {@code true} overrides any explicit name set
   * via {@link #setDatabaseName}.
   *
   * @see #setDatabaseName
   */
  public void setGenerateUniqueDatabaseName(boolean generateUniqueDatabaseName) {
    this.generateUniqueDatabaseName = generateUniqueDatabaseName;
  }

  /**
   * Set the name of the database.
   * <p>Defaults to {@value #DEFAULT_DATABASE_NAME}.
   * <p>Will be overridden if the {@code generateUniqueDatabaseName} flag
   * has been set to {@code true}.
   *
   * @param databaseName name of the embedded database
   * @see #setGenerateUniqueDatabaseName
   */
  public void setDatabaseName(String databaseName) {
    Assert.hasText(databaseName, "Database name is required");
    this.databaseName = databaseName;
  }

  /**
   * Set the factory to use to create the {@link DataSource} instance that
   * connects to the embedded database.
   * <p>Defaults to {@link SimpleDriverDataSourceFactory}.
   */
  public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
    Assert.notNull(dataSourceFactory, "DataSourceFactory is required");
    this.dataSourceFactory = dataSourceFactory;
  }

  /**
   * Set the type of embedded database to use.
   * <p>Call this when you wish to configure one of the pre-supported types.
   * <p>Defaults to HSQL.
   *
   * @param type the database type
   */
  public void setDatabaseType(EmbeddedDatabaseType type) {
    this.databaseConfigurer = EmbeddedDatabaseConfigurer.from(type);
  }

  /**
   * Set the strategy that will be used to configure the embedded database instance.
   * <p>Call this when you wish to use an embedded database type not already supported.
   */
  public void setDatabaseConfigurer(EmbeddedDatabaseConfigurer configurer) {
    this.databaseConfigurer = configurer;
  }

  /**
   * Set the strategy that will be used to initialize or populate the embedded
   * database.
   * <p>Defaults to {@code null}.
   */
  public void setDatabasePopulator(DatabasePopulator populator) {
    this.databasePopulator = populator;
  }

  /**
   * Factory method that returns the {@linkplain EmbeddedDatabase embedded database}
   * instance, which is also a {@link DataSource}.
   */
  public EmbeddedDatabase getDatabase() {
    if (this.dataSource == null) {
      initDatabase();
    }
    return new EmbeddedDataSourceProxy(this.dataSource);
  }

  /**
   * Hook to initialize the embedded database.
   * <p>If the {@code generateUniqueDatabaseName} flag has been set to {@code true},
   * the current value of the {@linkplain #setDatabaseName database name} will
   * be overridden with an auto-generated name.
   * <p>Subclasses may call this method to force initialization; however,
   * this method should only be invoked once.
   * <p>After calling this method, {@link #getDataSource()} returns the
   * {@link DataSource} providing connectivity to the database.
   */
  protected void initDatabase() {
    if (this.generateUniqueDatabaseName) {
      setDatabaseName(UUID.randomUUID().toString());
    }

    // Create the embedded database first
    if (this.databaseConfigurer == null) {
      this.databaseConfigurer = EmbeddedDatabaseConfigurer.from(EmbeddedDatabaseType.HSQL);
    }
    this.databaseConfigurer.configureConnectionProperties(
            this.dataSourceFactory.getConnectionProperties(), this.databaseName);
    this.dataSource = this.dataSourceFactory.getDataSource();

    if (logger.isInfoEnabled()) {
      if (this.dataSource instanceof SimpleDriverDataSource sdds) {
        logger.info("Starting embedded database: url='{}', username='{}'",
                sdds.getUrl(), sdds.getUsername());
      }
      else {
        logger.info("Starting embedded database '{}'", databaseName);
      }
    }

    // Now populate the database
    if (this.databasePopulator != null) {
      try {
        DatabasePopulator.execute(this.databasePopulator, this.dataSource);
      }
      catch (RuntimeException ex) {
        // failed to populate, so leave it as not initialized
        shutdownDatabase();
        throw ex;
      }
    }
  }

  /**
   * Hook to shutdown the embedded database. Subclasses may call this method
   * to force shutdown.
   * <p>After calling, {@link #getDataSource()} returns {@code null}.
   * <p>Does nothing if no embedded database has been initialized.
   */
  protected void shutdownDatabase() {
    if (this.dataSource != null) {
      if (logger.isInfoEnabled()) {
        if (this.dataSource instanceof SimpleDriverDataSource simple) {
          logger.info("Shutting down embedded database: url='{}'", simple.getUrl());
        }
        else {
          logger.info("Shutting down embedded database '{}'", this.databaseName);
        }
      }
      if (this.databaseConfigurer != null) {
        this.databaseConfigurer.shutdown(this.dataSource, this.databaseName);
      }
      this.dataSource = null;
    }
  }

  /**
   * Hook that gets the {@link DataSource} that provides the connectivity to the
   * embedded database.
   * <p>Returns {@code null} if the {@code DataSource} has not been initialized
   * or if the database has been shut down. Subclasses may call this method to
   * access the {@code DataSource} instance directly.
   */
  @Nullable
  protected final DataSource getDataSource() {
    return this.dataSource;
  }

  private class EmbeddedDataSourceProxy implements EmbeddedDatabase {

    private final DataSource dataSource;

    public EmbeddedDataSourceProxy(DataSource dataSource) {
      this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
      return this.dataSource.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
      return this.dataSource.getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
      return this.dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
      this.dataSource.setLogWriter(out);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
      return this.dataSource.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
      this.dataSource.setLoginTimeout(seconds);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      return this.dataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return this.dataSource.isWrapperFor(iface);
    }

    // getParentLogger() is required for JDBC 4.1 compatibility

    @Override
    public java.util.logging.Logger getParentLogger() {
      return java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
    }

    @Override
    public void shutdown() {
      shutdownDatabase();
    }
  }

}
