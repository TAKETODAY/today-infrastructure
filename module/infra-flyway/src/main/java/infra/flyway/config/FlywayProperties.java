/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.flyway.config;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.context.properties.ConfigurationProperties;
import infra.context.properties.NestedConfigurationProperty;
import infra.format.annotation.DurationUnit;

/**
 * Configuration properties for Flyway database migrations.
 *
 * @author Dave Syer
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Chris Bono
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@ConfigurationProperties("flyway")
public class FlywayProperties {

  /**
   * Whether to enable flyway.
   */
  public boolean enabled = true;

  /**
   * Whether to fail if a location of migration scripts doesn't exist.
   */
  public boolean failOnMissingLocations;

  /**
   * Locations of migrations scripts. Can contain the special "{vendor}" placeholder to
   * use vendor-specific locations.
   */
  public List<String> locations = new ArrayList<>(Collections.singletonList("classpath:db/migration"));

  /**
   * Locations of callbacks. Can contain the special "{vendor}" placeholder to use
   * vendor-specific callbacks. Unprefixed locations or locations starting with
   * "classpath:" point to a package on the classpath and may contain both SQL and
   * Java-based callbacks. Locations starting with "filesystem:" point to a directory on
   * the filesystem, may only contain SQL callbacks.
   */
  public List<String> callbackLocations = new ArrayList<>();

  /**
   * Encoding of SQL migrations.
   */
  public Charset encoding = StandardCharsets.UTF_8;

  /**
   * Maximum number of retries when attempting to connect to the database.
   */
  public int connectRetries;

  /**
   * Maximum time between retries when attempting to connect to the database. If a
   * duration suffix is not specified, seconds will be used.
   */
  @DurationUnit(ChronoUnit.SECONDS)
  public Duration connectRetriesInterval = Duration.ofSeconds(120);

  /**
   * Maximum number of retries when trying to obtain a lock.
   */
  public int lockRetryCount = 50;

  /**
   * Default schema name managed by Flyway (case-sensitive).
   */
  public @Nullable String defaultSchema;

  /**
   * Scheme names managed by Flyway (case-sensitive).
   */
  public List<String> schemas = new ArrayList<>();

  /**
   * Whether Flyway should attempt to create the schemas specified in the schemas
   * property.
   */
  public boolean createSchemas = true;

  /**
   * Name of the schema history table that will be used by Flyway.
   */
  public String table = "flyway_schema_history";

  /**
   * Tablespace in which the schema history table is created. Ignored when using a
   * database that does not support tablespaces. Defaults to the default tablespace of
   * the connection used by Flyway.
   */
  public @Nullable String tablespace;

  /**
   * Description to tag an existing schema with when applying a baseline.
   */
  public String baselineDescription = "<< Flyway Baseline >>";

  /**
   * Version to tag an existing schema with when executing baseline.
   */
  public String baselineVersion = "1";

  /**
   * Username recorded in the schema history table as having applied the migration.
   */
  public @Nullable String installedBy;

  /**
   * Placeholders and their replacements to apply to sql migration scripts.
   */
  public Map<String, String> placeholders = new HashMap<>();

  /**
   * Prefix of placeholders in migration scripts.
   */
  public String placeholderPrefix = "${";

  /**
   * Suffix of placeholders in migration scripts.
   */
  public String placeholderSuffix = "}";

  /**
   * Separator of default placeholders.
   */
  public String placeholderSeparator = ":";

  /**
   * Perform placeholder replacement in migration scripts.
   */
  public boolean placeholderReplacement = true;

  /**
   * File name prefix for SQL migrations.
   */
  public String sqlMigrationPrefix = "V";

  /**
   * File name suffix for SQL migrations.
   */
  public List<String> sqlMigrationSuffixes = new ArrayList<>(Collections.singleton(".sql"));

  /**
   * File name separator for SQL migrations.
   */
  public String sqlMigrationSeparator = "__";

  /**
   * File name prefix for repeatable SQL migrations.
   */
  public String repeatableSqlMigrationPrefix = "R";

  /**
   * Target version up to which migrations should be considered.
   */
  public String target = "latest";

  /**
   * Login user of the database to migrate.
   */
  public @Nullable String user;

  /**
   * Login password of the database to migrate.
   */
  public @Nullable String password;

  /**
   * Fully qualified name of the JDBC driver. Auto-detected based on the URL by default.
   */
  public @Nullable String driverClassName;

  /**
   * JDBC url of the database to migrate. If not set, the primary configured data source
   * is used.
   */
  public @Nullable String url;

  /**
   * SQL statements to execute to initialize a connection immediately after obtaining
   * it.
   */
  public List<String> initSqls = new ArrayList<>();

  /**
   * Whether to automatically call baseline when migrating a non-empty schema.
   */
  public boolean baselineOnMigrate;

  /**
   * Whether to disable cleaning of the database.
   */
  public boolean cleanDisabled = true;

  /**
   * Whether to group all pending migrations together in the same transaction when
   * applying them.
   */
  public boolean group;

  /**
   * Whether to allow mixing transactional and non-transactional statements within the
   * same migration.
   */
  public boolean mixed;

  /**
   * Whether to allow migrations to be run out of order.
   */
  public boolean outOfOrder;

  /**
   * Whether to skip default callbacks. If true, only custom callbacks are used.
   */
  public boolean skipDefaultCallbacks;

  /**
   * Whether to skip default resolvers. If true, only custom resolvers are used.
   */
  public boolean skipDefaultResolvers;

  /**
   * Whether to validate migrations and callbacks whose scripts do not obey the correct
   * naming convention.
   */
  public boolean validateMigrationNaming;

  /**
   * Whether to automatically call validate when performing a migration.
   */
  public boolean validateOnMigrate = true;

  /**
   * Prefix of placeholders in migration scripts.
   */
  public String scriptPlaceholderPrefix = "FP__";

  /**
   * Suffix of placeholders in migration scripts.
   */
  public String scriptPlaceholderSuffix = "__";

  /**
   * PowerShell executable to use for running PowerShell scripts. Default to
   * "powershell" on Windows, "pwsh" on other platforms.
   */
  public @Nullable String powershellExecutable;

  /**
   * Whether Flyway should execute SQL within a transaction.
   */
  public boolean executeInTransaction = true;

  /**
   * Loggers Flyway should use.
   */
  public String[] loggers = { "slf4j" };

  /**
   * Whether to batch SQL statements when executing them.
   */
  public @Nullable Boolean batch;

  /**
   * File to which the SQL statements of a migration dry run should be output. Requires
   * Flyway Teams.
   */
  public @Nullable File dryRunOutput;

  /**
   * Rules for the built-in error handling to override specific SQL states and error
   * codes. Requires Flyway Teams.
   */
  public String @Nullable [] errorOverrides;

  /**
   * Whether to stream SQL migrations when executing them.
   */
  public @Nullable Boolean stream;

  /**
   * Properties to pass to the JDBC driver.
   */
  public Map<String, String> jdbcProperties = new HashMap<>();

  /**
   * Path of the Kerberos config file. Requires Flyway Teams.
   */
  public @Nullable String kerberosConfigFile;

  /**
   * Whether Flyway should output a table with the results of queries when executing
   * migrations.
   */
  public @Nullable Boolean outputQueryResults;

  /**
   * Whether Flyway should skip executing the contents of the migrations and only update
   * the schema history table.
   */
  public @Nullable Boolean skipExecutingMigrations;

  /**
   * List of patterns that identify migrations to ignore when performing validation.
   */
  public List<String> ignoreMigrationPatterns = Collections.singletonList("*:future");

  /**
   * Whether to attempt to automatically detect SQL migration file encoding.
   */
  public @Nullable Boolean detectEncoding;

  /**
   * Whether to enable community database support.
   */
  public @Nullable Boolean communityDbSupportEnabled;

  @NestedConfigurationProperty
  public final Oracle oracle = new Oracle();

  @NestedConfigurationProperty
  public final Postgresql postgresql = new Postgresql();

  @NestedConfigurationProperty
  public final Sqlserver sqlserver = new Sqlserver();

  /**
   * {@code OracleConfigurationExtension} properties.
   */
  public static class Oracle {

    /**
     * Whether to enable support for Oracle SQL*Plus commands. Requires Flyway Teams.
     */
    public @Nullable Boolean sqlplus;

    /**
     * Whether to issue a warning rather than an error when a not-yet-supported Oracle
     * SQL*Plus statement is encountered. Requires Flyway Teams.
     */
    public @Nullable Boolean sqlplusWarn;

    /**
     * Path of the Oracle Kerberos cache file. Requires Flyway Teams.
     */
    public @Nullable String kerberosCacheFile;

    /**
     * Location of the Oracle Wallet, used to sign in to the database automatically.
     * Requires Flyway Teams.
     */
    public @Nullable String walletLocation;

  }

  /**
   * {@code PostgreSQLConfigurationExtension} properties.
   */
  public static class Postgresql {

    /**
     * Whether transactional advisory locks should be used. If set to false,
     * session-level locks are used instead.
     */
    public @Nullable Boolean transactionalLock;

  }

  /**
   * {@code SQLServerConfigurationExtension} properties.
   */
  public static class Sqlserver {

    /**
     * Path to the SQL Server Kerberos login file. Requires Flyway Teams.
     */
    public @Nullable String kerberosLoginFile;

  }

}
