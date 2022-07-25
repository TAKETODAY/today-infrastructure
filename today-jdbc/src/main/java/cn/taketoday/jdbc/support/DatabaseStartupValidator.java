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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.jdbc.CannotGetJdbcConnectionException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Bean that checks if a database has already started up. To be referenced
 * via "depends-on" from beans that depend on database startup, like a Hibernate
 * SessionFactory or custom data access objects that access a DataSource directly.
 *
 * <p>Useful to defer application initialization until a database has started up.
 * Particularly appropriate for waiting on a slowly starting Oracle database.
 *
 * @author Juergen Hoeller
 * @author Marten Deinum
 * @since 4.0
 */
public class DatabaseStartupValidator implements InitializingBean {

  /**
   * The default interval.
   */
  public static final int DEFAULT_INTERVAL = 1;

  /**
   * The default timeout.
   */
  public static final int DEFAULT_TIMEOUT = 60;

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private DataSource dataSource;

  @Nullable
  private String validationQuery;

  private int interval = DEFAULT_INTERVAL;

  private int timeout = DEFAULT_TIMEOUT;

  /**
   * Set the DataSource to validate.
   */
  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * Set the SQL query string to use for validation.
   */
  public void setValidationQuery(String validationQuery) {
    this.validationQuery = validationQuery;
  }

  /**
   * Set the interval between validation runs (in seconds).
   * Default is {@value #DEFAULT_INTERVAL}.
   */
  public void setInterval(int interval) {
    this.interval = interval;
  }

  /**
   * Set the timeout (in seconds) after which a fatal exception
   * will be thrown. Default is {@value #DEFAULT_TIMEOUT}.
   */
  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  /**
   * Check whether the validation query can be executed on a Connection
   * from the specified DataSource, with the specified interval between
   * checks, until the specified timeout.
   */
  @Override
  public void afterPropertiesSet() {
    if (this.dataSource == null) {
      throw new IllegalArgumentException("Property 'dataSource' is required");
    }

    try {
      boolean validated = false;
      long beginTime = System.currentTimeMillis();
      long deadLine = beginTime + TimeUnit.SECONDS.toMillis(this.timeout);
      SQLException latestEx = null;

      while (!validated && System.currentTimeMillis() < deadLine) {
        Connection con = null;
        Statement stmt = null;
        try {
          con = this.dataSource.getConnection();
          if (con == null) {
            throw new CannotGetJdbcConnectionException("Failed to execute validation: " +
                    "DataSource returned null from getConnection(): " + this.dataSource);
          }
          if (this.validationQuery == null) {
            validated = con.isValid(this.interval);
          }
          else {
            stmt = con.createStatement();
            stmt.execute(this.validationQuery);
            validated = true;
          }
        }
        catch (SQLException ex) {
          latestEx = ex;
          if (logger.isDebugEnabled()) {
            if (this.validationQuery != null) {
              logger.debug("Validation query [{}] threw exception", validationQuery, ex);
            }
            else {
              logger.debug("Validation check threw exception", ex);
            }
          }
          if (logger.isInfoEnabled()) {
            float rest = ((float) (deadLine - System.currentTimeMillis())) / 1000;
            if (rest > this.interval) {
              logger.info("Database has not started up yet - retrying in {} seconds (timeout in {} seconds)", interval, rest);
            }
          }
        }
        finally {
          JdbcUtils.closeStatement(stmt);
          JdbcUtils.closeConnection(con);
        }

        if (!validated) {
          TimeUnit.SECONDS.sleep(this.interval);
        }
      }

      if (!validated) {
        throw new CannotGetJdbcConnectionException(
                "Database has not started up within " + this.timeout + " seconds", latestEx);
      }

      if (logger.isInfoEnabled()) {
        float duration = ((float) (System.currentTimeMillis() - beginTime)) / 1000;
        logger.info("Database startup detected after {} seconds", duration);
      }
    }
    catch (InterruptedException ex) {
      // Re-interrupt current thread, to allow other threads to react.
      Thread.currentThread().interrupt();
    }
  }

}
