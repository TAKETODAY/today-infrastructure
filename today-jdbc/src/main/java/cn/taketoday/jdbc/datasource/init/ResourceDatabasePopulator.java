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

package cn.taketoday.jdbc.datasource.init;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sql.DataSource;

import cn.taketoday.core.io.EncodedResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Populates, initializes, or cleans up a database using SQL scripts defined in
 * external resources.
 *
 * <ul>
 * <li>Call {@link #addScript} to add a single SQL script location.
 * <li>Call {@link #addScripts} to add multiple SQL script locations.
 * <li>Consult the setter methods in this class for further configuration options.
 * <li>Call {@link #populate} or {@link #execute} to initialize or clean up the
 * database using the configured scripts.
 * </ul>
 *
 * @author Keith Donald
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Oliver Gierke
 * @author Sam Brannen
 * @author Chris Baldwin
 * @author Phillip Webb
 * @see DatabasePopulator
 * @see ScriptUtils
 * @since 4.0
 */
public class ResourceDatabasePopulator implements DatabasePopulator {

  ArrayList<Resource> scripts = new ArrayList<>();

  @Nullable
  private String sqlScriptEncoding;

  private String separator = ScriptUtils.DEFAULT_STATEMENT_SEPARATOR;

  private String[] commentPrefixes = ScriptUtils.DEFAULT_COMMENT_PREFIXES;

  private String blockCommentStartDelimiter = ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER;

  private String blockCommentEndDelimiter = ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER;

  private boolean continueOnError = false;

  private boolean ignoreFailedDrops = false;

  /**
   * Construct a new {@code ResourceDatabasePopulator} with default settings.
   */
  public ResourceDatabasePopulator() {
  }

  /**
   * Construct a new {@code ResourceDatabasePopulator} with default settings
   * for the supplied scripts.
   *
   * @param scripts the scripts to execute to initialize or clean up the database
   * (never {@code null})
   */
  public ResourceDatabasePopulator(Resource... scripts) {
    setScripts(scripts);
  }

  /**
   * Construct a new {@code ResourceDatabasePopulator} with the supplied values.
   *
   * @param continueOnError flag to indicate that all failures in SQL should be
   * logged but not cause a failure
   * @param ignoreFailedDrops flag to indicate that a failed SQL {@code DROP}
   * statement can be ignored
   * @param sqlScriptEncoding the encoding for the supplied SQL scripts
   * (may be {@code null} or <em>empty</em> to indicate platform encoding)
   * @param scripts the scripts to execute to initialize or clean up the database
   * (never {@code null})
   */
  public ResourceDatabasePopulator(
          boolean continueOnError, boolean ignoreFailedDrops,
          @Nullable String sqlScriptEncoding, Resource... scripts) {

    this.continueOnError = continueOnError;
    this.ignoreFailedDrops = ignoreFailedDrops;
    setSqlScriptEncoding(sqlScriptEncoding);
    setScripts(scripts);
  }

  /**
   * Add a script to execute to initialize or clean up the database.
   *
   * @param script the path to an SQL script (never {@code null})
   */
  public void addScript(Resource script) {
    Assert.notNull(script, "'script' must not be null");
    this.scripts.add(script);
  }

  /**
   * Add multiple scripts to execute to initialize or clean up the database.
   *
   * @param scripts the scripts to execute (never {@code null})
   */
  public void addScripts(Resource... scripts) {
    assertContentsOfScriptArray(scripts);
    this.scripts.addAll(Arrays.asList(scripts));
  }

  /**
   * Set the scripts to execute to initialize or clean up the database,
   * replacing any previously added scripts.
   *
   * @param scripts the scripts to execute (never {@code null})
   */
  public void setScripts(Resource... scripts) {
    assertContentsOfScriptArray(scripts);
    // Ensure that the list is modifiable
    this.scripts = new ArrayList<>(Arrays.asList(scripts));
  }

  private void assertContentsOfScriptArray(Resource... scripts) {
    Assert.notNull(scripts, "'scripts' must not be null");
    Assert.noNullElements(scripts, "'scripts' must not contain null elements");
  }

  /**
   * Specify the encoding for the configured SQL scripts,
   * if different from the platform encoding.
   *
   * @param sqlScriptEncoding the encoding used in scripts
   * (may be {@code null} or empty to indicate platform encoding)
   * @see #addScript(Resource)
   */
  public void setSqlScriptEncoding(@Nullable String sqlScriptEncoding) {
    this.sqlScriptEncoding = (StringUtils.hasText(sqlScriptEncoding) ? sqlScriptEncoding : null);
  }

  /**
   * Specify the statement separator, if a custom one.
   * <p>Defaults to {@code ";"} if not specified and falls back to {@code "\n"}
   * as a last resort; may be set to {@link ScriptUtils#EOF_STATEMENT_SEPARATOR}
   * to signal that each script contains a single statement without a separator.
   *
   * @param separator the script statement separator
   */
  public void setSeparator(String separator) {
    this.separator = separator;
  }

  /**
   * Set the prefix that identifies single-line comments within the SQL scripts.
   * <p>Defaults to {@code "--"}.
   *
   * @param commentPrefix the prefix for single-line comments
   * @see #setCommentPrefixes(String...)
   */
  public void setCommentPrefix(String commentPrefix) {
    Assert.hasText(commentPrefix, "'commentPrefix' must not be null or empty");
    this.commentPrefixes = new String[] { commentPrefix };
  }

  /**
   * Set the prefixes that identify single-line comments within the SQL scripts.
   * <p>Defaults to {@code ["--"]}.
   *
   * @param commentPrefixes the prefixes for single-line comments
   */
  public void setCommentPrefixes(String... commentPrefixes) {
    Assert.notEmpty(commentPrefixes, "'commentPrefixes' must not be null or empty");
    Assert.noNullElements(commentPrefixes, "'commentPrefixes' must not contain null elements");
    this.commentPrefixes = commentPrefixes;
  }

  /**
   * Set the start delimiter that identifies block comments within the SQL
   * scripts.
   * <p>Defaults to {@code "/*"}.
   *
   * @param blockCommentStartDelimiter the start delimiter for block comments
   * (never {@code null} or empty)
   * @see #setBlockCommentEndDelimiter
   */
  public void setBlockCommentStartDelimiter(String blockCommentStartDelimiter) {
    Assert.hasText(blockCommentStartDelimiter, "'blockCommentStartDelimiter' must not be null or empty");
    this.blockCommentStartDelimiter = blockCommentStartDelimiter;
  }

  /**
   * Set the end delimiter that identifies block comments within the SQL
   * scripts.
   * <p>Defaults to <code>"*&#47;"</code>.
   *
   * @param blockCommentEndDelimiter the end delimiter for block comments
   * (never {@code null} or empty)
   * @see #setBlockCommentStartDelimiter
   */
  public void setBlockCommentEndDelimiter(String blockCommentEndDelimiter) {
    Assert.hasText(blockCommentEndDelimiter, "'blockCommentEndDelimiter' must not be null or empty");
    this.blockCommentEndDelimiter = blockCommentEndDelimiter;
  }

  /**
   * Flag to indicate that all failures in SQL should be logged but not cause a failure.
   * <p>Defaults to {@code false}.
   *
   * @param continueOnError {@code true} if script execution should continue on error
   */
  public void setContinueOnError(boolean continueOnError) {
    this.continueOnError = continueOnError;
  }

  /**
   * Flag to indicate that a failed SQL {@code DROP} statement can be ignored.
   * <p>This is useful for a non-embedded database whose SQL dialect does not
   * support an {@code IF EXISTS} clause in a {@code DROP} statement.
   * <p>The default is {@code false} so that if the populator runs accidentally, it will
   * fail fast if a script starts with a {@code DROP} statement.
   *
   * @param ignoreFailedDrops {@code true} if failed drop statements should be ignored
   */
  public void setIgnoreFailedDrops(boolean ignoreFailedDrops) {
    this.ignoreFailedDrops = ignoreFailedDrops;
  }

  /**
   * {@inheritDoc}
   *
   * @see #execute(DataSource)
   */
  @Override
  public void populate(Connection connection) throws ScriptException {
    Assert.notNull(connection, "'connection' must not be null");
    for (Resource script : this.scripts) {
      EncodedResource encodedScript = new EncodedResource(script, this.sqlScriptEncoding);
      ScriptUtils.executeSqlScript(connection, encodedScript, this.continueOnError, this.ignoreFailedDrops,
              this.commentPrefixes, this.separator, this.blockCommentStartDelimiter, this.blockCommentEndDelimiter);
    }
  }

  /**
   * Execute this {@code ResourceDatabasePopulator} against the given
   * {@link DataSource}.
   * <p>Delegates to {@link DatabasePopulator#execute}.
   *
   * @param dataSource the {@code DataSource} to execute against (never {@code null})
   * @throws ScriptException if an error occurs
   * @see #populate(Connection)
   */
  public void execute(DataSource dataSource) throws ScriptException {
    DatabasePopulator.execute(this, dataSource);
  }

}
