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

package infra.jdbc.datasource.embedded;

import javax.sql.DataSource;

import infra.core.io.DefaultResourceLoader;
import infra.core.io.ResourceLoader;
import infra.jdbc.datasource.init.DatabasePopulator;
import infra.jdbc.datasource.init.ResourceDatabasePopulator;
import infra.jdbc.datasource.init.ScriptUtils;
import infra.lang.Assert;

/**
 * A builder that provides a convenient API for constructing an embedded database.
 *
 * <h3>Usage Example</h3>
 * <pre class="code">
 * EmbeddedDatabase db = new EmbeddedDatabaseBuilder()
 *     .generateUniqueName(true)
 *     .setType(H2)
 *     .setScriptEncoding("UTF-8")
 *     .ignoreFailedDrops(true)
 *     .addScript("schema.sql")
 *     .addScripts("user_data.sql", "country_data.sql")
 *     .build();
 *
 * // perform actions against the db (EmbeddedDatabase extends javax.sql.DataSource)
 *
 * db.shutdown();
 * </pre>
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ScriptUtils
 * @see ResourceDatabasePopulator
 * @see DatabasePopulator
 * @since 4.0
 */
public class EmbeddedDatabaseBuilder {

  private final EmbeddedDatabaseFactory databaseFactory;

  private final ResourceDatabasePopulator databasePopulator;

  private final ResourceLoader resourceLoader;

  /**
   * Create a new embedded database builder with a {@link DefaultResourceLoader}.
   */
  public EmbeddedDatabaseBuilder() {
    this(new DefaultResourceLoader());
  }

  /**
   * Create a new embedded database builder with the given {@link ResourceLoader}.
   *
   * @param resourceLoader the {@code ResourceLoader} to delegate to
   */
  public EmbeddedDatabaseBuilder(ResourceLoader resourceLoader) {
    this.databaseFactory = new EmbeddedDatabaseFactory();
    this.databasePopulator = new ResourceDatabasePopulator();
    this.databaseFactory.setDatabasePopulator(this.databasePopulator);
    this.resourceLoader = resourceLoader;
  }

  /**
   * Specify whether a unique ID should be generated and used as the database name.
   * <p>If the configuration for this builder is reused across multiple
   * application contexts within a single JVM, this flag should be <em>enabled</em>
   * (i.e., set to {@code true}) in order to ensure that each application context
   * gets its own embedded database.
   * <p>Enabling this flag overrides any explicit name set via {@link #setName}.
   *
   * @param flag {@code true} if a unique database name should be generated
   * @return {@code this}, to facilitate method chaining
   * @see #setName
   */
  public EmbeddedDatabaseBuilder generateUniqueName(boolean flag) {
    this.databaseFactory.setGenerateUniqueDatabaseName(flag);
    return this;
  }

  /**
   * Set the name of the embedded database.
   * <p>Defaults to {@link EmbeddedDatabaseFactory#DEFAULT_DATABASE_NAME} if
   * not called.
   * <p>Will be overridden if the {@code generateUniqueName} flag has been
   * set to {@code true}.
   *
   * @param databaseName the name of the embedded database to build
   * @return {@code this}, to facilitate method chaining
   * @see #generateUniqueName
   */
  public EmbeddedDatabaseBuilder setName(String databaseName) {
    this.databaseFactory.setDatabaseName(databaseName);
    return this;
  }

  /**
   * Set the type of embedded database. Consider using {@link #setDatabaseConfigurer}
   * if customization of the connections properties is necessary.
   * <p>Defaults to HSQL if not called.
   *
   * @param databaseType the type of embedded database to build
   * @return {@code this}, to facilitate method chaining
   */
  public EmbeddedDatabaseBuilder setType(EmbeddedDatabaseType databaseType) {
    this.databaseFactory.setDatabaseType(databaseType);
    return this;
  }

  /**
   * Set the {@linkplain EmbeddedDatabaseConfigurer configurer} to use to
   * configure the embedded database, as an alternative to {@link #setType}.
   *
   * @param configurer the configurer of the embedded database
   * @return {@code this}, to facilitate method chaining
   * @see EmbeddedDatabaseConfigurer
   */
  public EmbeddedDatabaseBuilder setDatabaseConfigurer(EmbeddedDatabaseConfigurer configurer) {
    this.databaseFactory.setDatabaseConfigurer(configurer);
    return this;
  }

  /**
   * Set the factory to use to create the {@link DataSource} instance that
   * connects to the embedded database.
   * <p>Defaults to {@link SimpleDriverDataSourceFactory} but can be overridden,
   * for example to introduce connection pooling.
   *
   * @return {@code this}, to facilitate method chaining
   */
  public EmbeddedDatabaseBuilder setDataSourceFactory(DataSourceFactory dataSourceFactory) {
    Assert.notNull(dataSourceFactory, "DataSourceFactory is required");
    this.databaseFactory.setDataSourceFactory(dataSourceFactory);
    return this;
  }

  /**
   * Add default SQL scripts to execute to populate the database.
   * <p>The default scripts are {@code "schema.sql"} to create the database
   * schema and {@code "data.sql"} to populate the database with data.
   *
   * @return {@code this}, to facilitate method chaining
   */
  public EmbeddedDatabaseBuilder addDefaultScripts() {
    return addScripts("schema.sql", "data.sql");
  }

  /**
   * Add an SQL script to execute to initialize or populate the database.
   *
   * @param script the script to execute
   * @return {@code this}, to facilitate method chaining
   */
  public EmbeddedDatabaseBuilder addScript(String script) {
    this.databasePopulator.addScript(this.resourceLoader.getResource(script));
    return this;
  }

  /**
   * Add multiple SQL scripts to execute to initialize or populate the database.
   *
   * @param scripts the scripts to execute
   * @return {@code this}, to facilitate method chaining
   */
  public EmbeddedDatabaseBuilder addScripts(String... scripts) {
    for (String script : scripts) {
      addScript(script);
    }
    return this;
  }

  /**
   * Specify the character encoding used in all SQL scripts, if different from
   * the platform encoding.
   *
   * @param scriptEncoding the encoding used in scripts
   * @return {@code this}, to facilitate method chaining
   */
  public EmbeddedDatabaseBuilder setScriptEncoding(String scriptEncoding) {
    this.databasePopulator.setSqlScriptEncoding(scriptEncoding);
    return this;
  }

  /**
   * Specify the statement separator used in all SQL scripts, if a custom one.
   * <p>Defaults to {@code ";"} if not specified and falls back to {@code "\n"}
   * as a last resort; may be set to {@link ScriptUtils#EOF_STATEMENT_SEPARATOR}
   * to signal that each script contains a single statement without a separator.
   *
   * @param separator the statement separator
   * @return {@code this}, to facilitate method chaining
   */
  public EmbeddedDatabaseBuilder setSeparator(String separator) {
    this.databasePopulator.setSeparator(separator);
    return this;
  }

  /**
   * Specify the single-line comment prefix used in all SQL scripts.
   * <p>Defaults to {@code "--"}.
   *
   * @param commentPrefix the prefix for single-line comments
   * @return {@code this}, to facilitate method chaining
   * @see #setCommentPrefixes(String...)
   */
  public EmbeddedDatabaseBuilder setCommentPrefix(String commentPrefix) {
    this.databasePopulator.setCommentPrefix(commentPrefix);
    return this;
  }

  /**
   * Specify the prefixes that identify single-line comments within all SQL scripts.
   * <p>Defaults to {@code ["--"]}.
   *
   * @param commentPrefixes the prefixes for single-line comments
   * @return {@code this}, to facilitate method chaining
   */
  public EmbeddedDatabaseBuilder setCommentPrefixes(String... commentPrefixes) {
    this.databasePopulator.setCommentPrefixes(commentPrefixes);
    return this;
  }

  /**
   * Specify the start delimiter for block comments in all SQL scripts.
   * <p>Defaults to {@code "/*"}.
   *
   * @param blockCommentStartDelimiter the start delimiter for block comments
   * @return {@code this}, to facilitate method chaining
   * @see #setBlockCommentEndDelimiter
   */
  public EmbeddedDatabaseBuilder setBlockCommentStartDelimiter(String blockCommentStartDelimiter) {
    this.databasePopulator.setBlockCommentStartDelimiter(blockCommentStartDelimiter);
    return this;
  }

  /**
   * Specify the end delimiter for block comments in all SQL scripts.
   * <p>Defaults to <code>"*&#47;"</code>.
   *
   * @param blockCommentEndDelimiter the end delimiter for block comments
   * @return {@code this}, to facilitate method chaining
   * @see #setBlockCommentStartDelimiter
   */
  public EmbeddedDatabaseBuilder setBlockCommentEndDelimiter(String blockCommentEndDelimiter) {
    this.databasePopulator.setBlockCommentEndDelimiter(blockCommentEndDelimiter);
    return this;
  }

  /**
   * Specify that all failures which occur while executing SQL scripts should
   * be logged but should not cause a failure.
   * <p>Defaults to {@code false}.
   *
   * @param flag {@code true} if script execution should continue on error
   * @return {@code this}, to facilitate method chaining
   */
  public EmbeddedDatabaseBuilder continueOnError(boolean flag) {
    this.databasePopulator.setContinueOnError(flag);
    return this;
  }

  /**
   * Specify that a failed SQL {@code DROP} statement within an executed
   * script can be ignored.
   * <p>This is useful for a database whose SQL dialect does not support an
   * {@code IF EXISTS} clause in a {@code DROP} statement.
   * <p>The default is {@code false} so that {@link #build building} will fail
   * fast if a script starts with a {@code DROP} statement.
   *
   * @param flag {@code true} if failed drop statements should be ignored
   * @return {@code this}, to facilitate method chaining
   */
  public EmbeddedDatabaseBuilder ignoreFailedDrops(boolean flag) {
    this.databasePopulator.setIgnoreFailedDrops(flag);
    return this;
  }

  /**
   * Build the embedded database.
   *
   * @return the embedded database
   */
  public EmbeddedDatabase build() {
    return this.databaseFactory.getDatabase();
  }

}
