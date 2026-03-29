package infra.mariadb4j.config;

import org.jspecify.annotations.Nullable;

import infra.context.properties.ConfigurationProperties;

/**
 * Configuration properties for MariaDB4j embedded database.
 * <p>
 * These properties are bound from the environment using the {@code mariadb4j} prefix.
 * They allow customization of the MariaDB server instance, including ports, directories,
 * and runtime behavior.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/3/22 09:31
 */
@ConfigurationProperties("mariadb4j")
public class MariaDB4jProperties {

  public boolean enabled = true;

  /**
   * The port number on which the MariaDB server should listen.
   */
  public @Nullable Integer port;

  /**
   * The path to the Unix socket file used for local connections.
   */
  public @Nullable String socket;

  /**
   * The directory where MariaDB stores its data files.
   */
  public @Nullable String dataDir;

  /**
   * The directory used for temporary files during database operations.
   */
  public @Nullable String tmpDir;

  /**
   * The base installation directory for the MariaDB binaries and libraries.
   */
  public @Nullable String baseDir;

  /**
   * The directory containing MariaDB library files.
   */
  public @Nullable String libDir;

  /**
   * The operating system user account under which the MariaDB process should run.
   */
  public @Nullable String osUser;

  /**
   * The default character set to be used by the MariaDB server.
   */
  public @Nullable String defaultCharset;

  /**
   * Whether to unpack the embedded MariaDB distribution archive automatically.
   * Defaults to {@code true}.
   */
  public boolean unpack = true;

  /**
   * The name of the database to create automatically upon server startup.
   * If specified, a database with this name will be created if it does not already exist.
   */
  public @Nullable String createDatabase;

  /**
   * The classpath resource location of the SQL script to execute after the database is initialized.
   * This script is typically used for schema creation or initial data population.
   */
  public @Nullable String scriptResource;

}
