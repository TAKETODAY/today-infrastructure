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

package infra.annotation.config.jdbc;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import infra.context.properties.ConfigurationProperties;
import infra.format.annotation.DurationUnit;
import infra.lang.Nullable;

/**
 * Configuration properties for JDBC.
 *
 * @author Kazuki Shimizu
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/23 17:55
 */
@ConfigurationProperties(prefix = "jdbc")
public class JdbcProperties {

  private final Template template = new Template();

  public Template getTemplate() {
    return this.template;
  }

  /**
   * {@code JdbcTemplate} settings.
   */
  public static class Template {

    /**
     * Number of rows that should be fetched from the database when more rows are
     * needed. Use -1 to use the JDBC driver's default configuration.
     */
    private int fetchSize = -1;

    /**
     * Maximum number of rows. Use -1 to use the JDBC driver's default configuration.
     */
    private int maxRows = -1;

    /**
     * Query timeout. Default is to use the JDBC driver's default configuration. If a
     * duration suffix is not specified, seconds will be used.
     */
    @Nullable
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration queryTimeout;

    public int getFetchSize() {
      return this.fetchSize;
    }

    public void setFetchSize(int fetchSize) {
      this.fetchSize = fetchSize;
    }

    public int getMaxRows() {
      return this.maxRows;
    }

    public void setMaxRows(int maxRows) {
      this.maxRows = maxRows;
    }

    @Nullable
    public Duration getQueryTimeout() {
      return this.queryTimeout;
    }

    public void setQueryTimeout(@Nullable Duration queryTimeout) {
      this.queryTimeout = queryTimeout;
    }

  }

}
