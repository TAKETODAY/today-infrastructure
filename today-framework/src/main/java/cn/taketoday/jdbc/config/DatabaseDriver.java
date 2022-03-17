/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jdbc.config;

import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import javax.sql.DataSource;

import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Enumeration of common database drivers.
 *
 * @author Phillip Webb
 * @author Maciej Walkowiak
 * @author Marten Deinum
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/23 17:15
 */
public enum DatabaseDriver {

  /**
   * Unknown type.
   */
  UNKNOWN(null, null),

  /**
   * Apache Derby.
   */
  DERBY("Apache Derby",
          "org.apache.derby.jdbc.EmbeddedDriver",
          "org.apache.derby.jdbc.EmbeddedXADataSource",
          "SELECT 1 FROM SYSIBM.SYSDUMMY1"),

  /**
   * H2.
   */
  H2("H2",
          "org.h2.Driver",
          "org.h2.jdbcx.JdbcDataSource",
          "SELECT 1"),

  /**
   * HyperSQL DataBase.
   */
  HSQLDB("HSQL Database Engine",
          "org.hsqldb.jdbc.JDBCDriver",
          "org.hsqldb.jdbc.pool.JDBCXADataSource",
          "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SYSTEM_USERS"),

  /**
   * SQL Lite.
   */
  SQLITE("SQLite", "org.sqlite.JDBC"),

  /**
   * MySQL.
   */
  MYSQL("MySQL",
          "com.mysql.cj.jdbc.Driver",
          "com.mysql.cj.jdbc.MysqlXADataSource",
          "/* ping */ SELECT 1"),

  /**
   * Maria DB.
   */
  MARIADB("MariaDB",
          "org.mariadb.jdbc.Driver",
          "org.mariadb.jdbc.MariaDbDataSource", "SELECT 1"),

  /**
   * Google App Engine.
   */
  GAE(null,
          "com.google.appengine.api.rdbms.AppEngineDriver"),

  /**
   * Oracle.
   */
  ORACLE("Oracle",
          "oracle.jdbc.OracleDriver",
          "oracle.jdbc.xa.client.OracleXADataSource",
          "SELECT 'Hello' from DUAL"),

  /**
   * Postgres.
   */
  POSTGRESQL("PostgreSQL",
          "org.postgresql.Driver",
          "org.postgresql.xa.PGXADataSource",
          "SELECT 1"),

  /**
   * Amazon Redshift.
   */
  REDSHIFT("Redshift",
          "com.amazon.redshift.jdbc.Driver",
          null, "SELECT 1"),

  /**
   * HANA - SAP HANA Database - HDB.
   */
  HANA("HDB",
          "com.sap.db.jdbc.Driver",
          "com.sap.db.jdbcext.XADataSourceSAP",
          "SELECT 1 FROM SYS.DUMMY") {
    @Override
    protected Collection<String> getUrlPrefixes() {
      return Collections.singleton("sap");
    }
  },

  /**
   * jTDS. As it can be used for several databases, there isn't a single product name we
   * could rely on.
   */
  JTDS(null,
          "net.sourceforge.jtds.jdbc.Driver"),

  /**
   * SQL Server.
   */
  SQLSERVER("Microsoft SQL Server",
          "com.microsoft.sqlserver.jdbc.SQLServerDriver",
          "com.microsoft.sqlserver.jdbc.SQLServerXADataSource",
          "SELECT 1") {
    @Override
    protected boolean matchProductName(String productName) {
      return super.matchProductName(productName) || "SQL SERVER".equalsIgnoreCase(productName);
    }

  },

  /**
   * Firebird.
   */
  FIREBIRD("Firebird",
          "org.firebirdsql.jdbc.FBDriver",
          "org.firebirdsql.ds.FBXADataSource",
          "SELECT 1 FROM RDB$DATABASE") {
    @Override
    protected Collection<String> getUrlPrefixes() {
      return Arrays.asList("firebirdsql", "firebird");
    }

    @Override
    protected boolean matchProductName(String productName) {
      return super.matchProductName(productName)
              || productName.toLowerCase(Locale.ENGLISH).startsWith("firebird");
    }
  },

  /**
   * DB2 Server.
   */
  DB2("DB2",
          "com.ibm.db2.jcc.DB2Driver",
          "com.ibm.db2.jcc.DB2XADataSource",
          "SELECT 1 FROM SYSIBM.SYSDUMMY1") {
    @Override
    protected boolean matchProductName(String productName) {
      return super.matchProductName(productName) || productName.toLowerCase(Locale.ENGLISH).startsWith("db2/");
    }
  },

  /**
   * DB2 AS400 Server.
   */
  DB2_AS400("DB2 UDB for AS/400",
          "com.ibm.as400.access.AS400JDBCDriver",
          "com.ibm.as400.access.AS400JDBCXADataSource",
          "SELECT 1 FROM SYSIBM.SYSDUMMY1") {
    @Override
    public String getId() {
      return "db2";
    }

    @Override
    protected Collection<String> getUrlPrefixes() {
      return Collections.singleton("as400");
    }

    @Override
    protected boolean matchProductName(String productName) {
      return super.matchProductName(productName) || productName.toLowerCase(Locale.ENGLISH).contains("as/400");
    }
  },

  /**
   * Teradata.
   */
  TERADATA("Teradata",
          "com.teradata.jdbc.TeraDriver"),

  /**
   * Informix.
   */
  INFORMIX("Informix Dynamic Server",
          "com.informix.jdbc.IfxDriver",
          null,
          "select count(*) from systables") {
    @Override
    protected Collection<String> getUrlPrefixes() {
      return Arrays.asList("informix-sqli", "informix-direct");
    }

  },

  /**
   * Apache Phoenix.
   */
  PHOENIX("Apache Phoenix",
          "org.apache.phoenix.jdbc.PhoenixDriver",
          null,
          "SELECT 1 FROM SYSTEM.CATALOG LIMIT 1"),

  /**
   * Testcontainers.
   */
  TESTCONTAINERS(null,
          "org.testcontainers.jdbc.ContainerDatabaseDriver") {
    @Override
    protected Collection<String> getUrlPrefixes() {
      return Collections.singleton("tc");
    }

  };

  @Nullable
  private final String productName;

  @Nullable
  private final String driverClassName;

  @Nullable
  private final String xaDataSourceClassName;

  @Nullable
  private final String validationQuery;

  DatabaseDriver(@Nullable String productName, @Nullable String driverClassName) {
    this(productName, driverClassName, null);
  }

  DatabaseDriver(@Nullable String productName, @Nullable String driverClassName, @Nullable String xaDataSourceClassName) {
    this(productName, driverClassName, xaDataSourceClassName, null);
  }

  DatabaseDriver(@Nullable String productName, @Nullable String driverClassName, @Nullable String xaDataSourceClassName, @Nullable String validationQuery) {
    this.productName = productName;
    this.driverClassName = driverClassName;
    this.validationQuery = validationQuery;
    this.xaDataSourceClassName = xaDataSourceClassName;
  }

  /**
   * Return the identifier of this driver.
   *
   * @return the identifier
   */
  public String getId() {
    return name().toLowerCase(Locale.ENGLISH);
  }

  protected boolean matchProductName(String productName) {
    return this.productName != null && this.productName.equalsIgnoreCase(productName);
  }

  protected Collection<String> getUrlPrefixes() {
    return Collections.singleton(name().toLowerCase(Locale.ENGLISH));
  }

  /**
   * Return the driver class name.
   *
   * @return the class name or {@code null}
   */
  @Nullable
  public String getDriverClassName() {
    return this.driverClassName;
  }

  /**
   * Return the XA driver source class name.
   *
   * @return the class name or {@code null}
   */
  @Nullable
  public String getXaDataSourceClassName() {
    return this.xaDataSourceClassName;
  }

  /**
   * Return the validation query.
   *
   * @return the validation query or {@code null}
   */
  @Nullable
  public String getValidationQuery() {
    return this.validationQuery;
  }

  /**
   * Find a {@link DatabaseDriver} for the given URL.
   *
   * @param url the JDBC URL
   * @return the database driver or {@link #UNKNOWN} if not found
   */
  public static DatabaseDriver fromJdbcUrl(@Nullable String url) {
    if (StringUtils.isNotEmpty(url)) {
      Assert.isTrue(url.startsWith("jdbc"), "URL must start with 'jdbc'");
      String urlWithoutPrefix = url.substring("jdbc".length()).toLowerCase(Locale.ENGLISH);
      for (DatabaseDriver driver : values()) {
        for (String urlPrefix : driver.getUrlPrefixes()) {
          String prefix = ":" + urlPrefix + ":";
          if (driver != UNKNOWN && urlWithoutPrefix.startsWith(prefix)) {
            return driver;
          }
        }
      }
    }
    return UNKNOWN;
  }

  /**
   * Find a {@link DatabaseDriver} for the given product name.
   *
   * @param productName product name
   * @return the database driver or {@link #UNKNOWN} if not found
   */
  public static DatabaseDriver fromProductName(@Nullable String productName) {
    if (StringUtils.isNotEmpty(productName)) {
      for (DatabaseDriver candidate : values()) {
        if (candidate.matchProductName(productName)) {
          return candidate;
        }
      }
    }
    return UNKNOWN;
  }

  /**
   * Find a {@link DatabaseDriver} for the given {@code DataSource}.
   *
   * @param dataSource data source to inspect
   * @return the database driver of {@link #UNKNOWN} if not found
   */
  public static DatabaseDriver fromDataSource(DataSource dataSource) {
    try {
      String productName = JdbcUtils.commonDatabaseName(
              JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getDatabaseProductName));
      return DatabaseDriver.fromProductName(productName);
    }
    catch (Exception ex) {
      return DatabaseDriver.UNKNOWN;
    }
  }

}
