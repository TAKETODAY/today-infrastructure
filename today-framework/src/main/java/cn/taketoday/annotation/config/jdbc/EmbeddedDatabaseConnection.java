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

package cn.taketoday.annotation.config.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.sql.DataSource;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.config.DatabaseDriver;
import cn.taketoday.jdbc.core.ConnectionCallback;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Connection details for {@link EmbeddedDatabaseType embedded databases}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Nidhi Desai
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #get(ClassLoader)
 * @since 4.0 2022/2/23 17:14
 */
public enum EmbeddedDatabaseConnection {

  /**
   * No Connection.
   */
  NONE(null, null, null, (url) -> false),

  /**
   * H2 Database Connection.
   */
  H2(EmbeddedDatabaseType.H2, DatabaseDriver.H2.getDriverClassName(),
          "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", (url) -> url.contains(":h2:mem")),

  /**
   * Derby Database Connection.
   */
  DERBY(EmbeddedDatabaseType.DERBY, DatabaseDriver.DERBY.getDriverClassName(), "jdbc:derby:memory:%s;create=true",
          (url) -> true),

  /**
   * HSQL Database Connection.
   */
  HSQLDB(EmbeddedDatabaseType.HSQL, DatabaseDriver.HSQLDB.getDriverClassName(), "org.hsqldb.jdbcDriver",
          "jdbc:hsqldb:mem:%s", (url) -> url.contains(":hsqldb:mem:"));

  @Nullable
  private final EmbeddedDatabaseType type;

  private final String driverClass;

  @Nullable
  private final String alternativeDriverClass;

  @Nullable
  private final String url;

  private final Predicate<String> embeddedUrl;

  EmbeddedDatabaseConnection(EmbeddedDatabaseType type, String driverClass,
          String url, Predicate<String> embeddedUrl) {
    this(type, driverClass, null, url, embeddedUrl);
  }

  EmbeddedDatabaseConnection(EmbeddedDatabaseType type,
          String driverClass, String fallbackDriverClass, String url,
          Predicate<String> embeddedUrl) {
    this.type = type;
    this.driverClass = driverClass;
    this.alternativeDriverClass = fallbackDriverClass;
    this.url = url;
    this.embeddedUrl = embeddedUrl;
  }

  /**
   * Returns the driver class name.
   *
   * @return the driver class name
   */
  public String getDriverClassName() {
    return this.driverClass;
  }

  /**
   * Returns the {@link EmbeddedDatabaseType} for the connection.
   *
   * @return the database type
   */
  @Nullable
  public EmbeddedDatabaseType getType() {
    return this.type;
  }

  /**
   * Returns the URL for the connection using the specified {@code databaseName}.
   *
   * @param databaseName the name of the database
   * @return the connection URL
   */
  @Nullable
  public String getUrl(String databaseName) {
    Assert.hasText(databaseName, "DatabaseName must not be empty");
    return url != null ? String.format(this.url, databaseName) : null;
  }

  boolean isEmbeddedUrl(String url) {
    return this.embeddedUrl.test(url);
  }

  boolean isDriverCompatible(@Nullable String driverClass) {
    return (driverClass != null
            && (driverClass.equals(this.driverClass) || driverClass.equals(this.alternativeDriverClass)));
  }

  /**
   * Convenience method to determine if a given driver class name and url represent an
   * embedded database type.
   *
   * @param driverClass the driver class
   * @param url the jdbc url (can be {@code null})
   * @return true if the driver class and url refer to an embedded database
   */
  public static boolean isEmbedded(@Nullable String driverClass, @Nullable String url) {
    if (driverClass == null) {
      return false;
    }
    EmbeddedDatabaseConnection connection = getEmbeddedDatabaseConnection(driverClass);
    if (connection == NONE) {
      return false;
    }
    return (url == null || connection.isEmbeddedUrl(url));
  }

  private static EmbeddedDatabaseConnection getEmbeddedDatabaseConnection(String driverClass) {
    return Stream.of(H2, HSQLDB, DERBY).filter((connection) -> connection.isDriverCompatible(driverClass))
            .findFirst().orElse(NONE);
  }

  /**
   * Convenience method to determine if a given data source represents an embedded
   * database type.
   *
   * @param dataSource the data source to interrogate
   * @return true if the data source is one of the embedded types
   */
  public static boolean isEmbedded(DataSource dataSource) {
    try {
      return Boolean.TRUE.equals(new JdbcTemplate(dataSource).execute(new IsEmbedded()));
    }
    catch (DataAccessException ex) {
      // Could not connect, which means it's not embedded
      return false;
    }
  }

  /**
   * Returns the most suitable {@link EmbeddedDatabaseConnection} for the given class
   * loader.
   *
   * @param classLoader the class loader used to check for classes
   * @return an {@link EmbeddedDatabaseConnection} or {@link #NONE}.
   */
  public static EmbeddedDatabaseConnection get(@Nullable ClassLoader classLoader) {
    for (EmbeddedDatabaseConnection candidate : EmbeddedDatabaseConnection.values()) {
      if (candidate != NONE && ClassUtils.isPresent(candidate.getDriverClassName(), classLoader)) {
        return candidate;
      }
    }
    return NONE;
  }

  /**
   * {@link ConnectionCallback} to determine if a connection is embedded.
   */
  private static class IsEmbedded implements ConnectionCallback<Boolean> {

    @Override
    public Boolean doInConnection(Connection connection) throws SQLException, DataAccessException {
      DatabaseMetaData metaData = connection.getMetaData();
      String productName = metaData.getDatabaseProductName();
      if (productName == null) {
        return false;
      }
      productName = productName.toUpperCase(Locale.ENGLISH);
      EmbeddedDatabaseConnection[] candidates = EmbeddedDatabaseConnection.values();
      for (EmbeddedDatabaseConnection candidate : candidates) {
        if (candidate != NONE && productName.contains(candidate.getType().name())) {
          String url = metaData.getURL();
          return (url == null || candidate.isEmbeddedUrl(url));
        }
      }
      return false;
    }

  }

}

