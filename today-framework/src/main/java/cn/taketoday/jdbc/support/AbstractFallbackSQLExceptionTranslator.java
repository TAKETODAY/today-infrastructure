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

import java.sql.SQLException;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Base class for {@link SQLExceptionTranslator} implementations that allow for
 * fallback to some other {@link SQLExceptionTranslator}.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AbstractFallbackSQLExceptionTranslator implements SQLExceptionTranslator {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private SQLExceptionTranslator fallbackTranslator;

  /**
   * Override the default SQL state fallback translator
   * (typically a {@link SQLStateSQLExceptionTranslator}).
   */
  public void setFallbackTranslator(@Nullable SQLExceptionTranslator fallback) {
    this.fallbackTranslator = fallback;
  }

  /**
   * Return the fallback exception translator, if any.
   */
  @Nullable
  public SQLExceptionTranslator getFallbackTranslator() {
    return this.fallbackTranslator;
  }

  /**
   * Pre-checks the arguments, calls {@link #doTranslate}, and invokes the
   * {@link #getFallbackTranslator() fallback translator} if necessary.
   */
  @Override
  @Nullable
  public DataAccessException translate(String task, @Nullable String sql, SQLException ex) {
    Assert.notNull(ex, "Cannot translate a null SQLException");

    DataAccessException dae = doTranslate(task, sql, ex);
    if (dae != null) {
      // Specific exception match found.
      return dae;
    }

    // Looking for a fallback...
    SQLExceptionTranslator fallback = getFallbackTranslator();
    if (fallback != null) {
      return fallback.translate(task, sql, ex);
    }

    return null;
  }

  /**
   * Template method for actually translating the given exception.
   * <p>The passed-in arguments will have been pre-checked. Furthermore, this method
   * is allowed to return {@code null} to indicate that no exception match has
   * been found and that fallback translation should kick in.
   *
   * @param task readable text describing the task being attempted
   * @param sql the SQL query or update that caused the problem (if known)
   * @param ex the offending {@code SQLException}
   * @return the DataAccessException, wrapping the {@code SQLException};
   * or {@code null} if no exception match found
   */
  @Nullable
  protected abstract DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex);

  /**
   * Build a message {@code String} for the given {@link java.sql.SQLException}.
   * <p>To be called by translator subclasses when creating an instance of a generic
   * {@link cn.taketoday.dao.DataAccessException} class.
   *
   * @param task readable text describing the task being attempted
   * @param sql the SQL statement that caused the problem
   * @param ex the offending {@code SQLException}
   * @return the message {@code String} to use
   */
  protected String buildMessage(String task, @Nullable String sql, SQLException ex) {
    return task + "; " + (sql != null ? ("SQL [" + sql + "]; ") : "") + ex.getMessage();
  }

}
