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

package infra.session.config;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import infra.context.properties.ConfigurationProperties;
import infra.context.properties.NestedConfigurationProperty;
import infra.core.ApplicationHome;
import infra.core.ApplicationTemp;
import infra.format.annotation.DurationUnit;
import infra.util.Assert;
import infra.util.InfraStrategies;

/**
 * Configuration properties for server-side sessions, bound under
 * {@code server.session}.
 * <p>Configure session timeouts, persistence, storage limits, session ID length,
 * and session cookies.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties(prefix = "server.session", ignoreUnknownFields = true)
public class SessionProperties {

  private static final String SESSION_TEMP_DIR = InfraStrategies.getProperty(
          "infra.server.session.temp-dir", "server-sessions");

  /**
   * Session timeout. Defaults to 30 minutes. Values without a duration suffix
   * are interpreted as seconds.
   */
  @DurationUnit(ChronoUnit.SECONDS)
  private @Nullable Duration timeout = Duration.ofMinutes(30);

  /**
   * Whether to persist session data between restarts. Defaults to {@code false}.
   */
  private boolean persistent;

  /**
   * Directory used to store session data. Relative paths are resolved against
   * the application home directory. If not configured, an application-specific
   * temporary directory is used.
   */
  private @Nullable File storeDir;

  /**
   * Session cookie configuration.
   */
  @NestedConfigurationProperty
  public final CookieProperties cookie = new CookieProperties();

  /**
   * Session ID length. Defaults to 32 and must be greater than zero.
   */
  private int sessionIdLength = 32;

  /**
   * Maximum number of sessions that can be stored. Defaults to 10000.
   * Once the limit is reached, attempting to store an additional session
   * results in an {@link IllegalStateException}.
   */
  private int maxSessions = 10000;

  /**
   * Set the maximum number of sessions that can be stored. Once the limit is
   * reached, any attempt to store an additional session will result in an
   * {@link IllegalStateException}.
   * <p>Defaults to 10000.
   *
   * @param maxSessions the maximum number of sessions
   */
  public void setMaxSessions(int maxSessions) {
    this.maxSessions = maxSessions;
  }

  /**
   * Return the maximum number of sessions that can be stored.
   *
   * @return the maximum number of sessions, by default 10000
   */
  public int getMaxSessions() {
    return this.maxSessions;
  }

  /**
   * Set the session ID length.
   *
   * @param sessionIdLength the session ID length, which must be greater than zero
   * @throws IllegalArgumentException if the length is zero or negative
   */
  public void setSessionIdLength(int sessionIdLength) {
    Assert.isTrue(sessionIdLength > 0, "Session id length must > 0");
    this.sessionIdLength = sessionIdLength;
  }

  /**
   * Return the session ID length.
   *
   * @return the session ID length, by default 32
   */
  public int getSessionIdLength() {
    return sessionIdLength;
  }

  /**
   * Return the configured session timeout.
   *
   * @return the session timeout, by default 30 minutes, or {@code null}
   */
  public @Nullable Duration getTimeout() {
    return this.timeout;
  }

  /**
   * Set the session timeout.
   *
   * @param timeout the session timeout, or {@code null} to clear the configured value
   */
  public void setTimeout(@Nullable Duration timeout) {
    this.timeout = timeout;
  }

  /**
   * Return whether to persist session data between restarts.
   *
   * @return {@code true} if persistence is enabled; {@code false} by default
   */
  public boolean isPersistent() {
    return this.persistent;
  }

  /**
   * Set whether to persist session data between restarts.
   *
   * @param persistent {@code true} to enable session persistence
   */
  public void setPersistent(boolean persistent) {
    this.persistent = persistent;
  }

  /**
   * Return the configured session data store directory without resolving or
   * creating it.
   *
   * @return the configured directory, or {@code null} to use an
   * application-specific temporary directory
   * @see #getValidStoreDir(ApplicationTemp)
   */
  public @Nullable File getStoreDir() {
    return this.storeDir;
  }

  /**
   * Set the directory used to store session data.
   * <p>Relative paths are resolved against the application home directory when
   * {@link #getValidStoreDir(ApplicationTemp)} is called.
   *
   * @param storeDir the session data store directory, or {@code null} to use an
   * application-specific temporary directory
   */
  public void setStoreDir(@Nullable File storeDir) {
    this.storeDir = storeDir;
  }

  /**
   * Resolve the session data store directory, creating it if necessary.
   * <p>Equivalent to {@link #getValidStoreDir(ApplicationTemp, boolean)
   * getValidStoreDir(applicationTemp, true)}.
   *
   * @param applicationTemp the application temporary directory provider, or
   * {@code null} to use {@link ApplicationTemp#instance}
   * @return the resolved session data store directory
   * @throws IllegalStateException if the configured path points to a file or
   * the directory does not exist after an attempt to create it
   * @throws java.io.UncheckedIOException if creating the default temporary
   * directory fails
   */
  public File getValidStoreDir(@Nullable ApplicationTemp applicationTemp) {
    return getValidStoreDir(applicationTemp, true);
  }

  /**
   * Resolve the configured session data store directory.
   * <p>Delegates to {@link #getValidStoreDir(ApplicationTemp, File, boolean)}
   * with the configured {@linkplain #getStoreDir() store directory}.
   *
   * @param applicationTemp the application temporary directory provider, or
   * {@code null} to use {@link ApplicationTemp#instance}
   * @param mkdirs whether to create the configured directory if it does not
   * exist; does not prevent creation of the default temporary directory
   * @return the resolved directory, which may not exist if {@code mkdirs} is
   * {@code false} and a store directory is configured
   * @throws IllegalStateException if the configured path points to a file, or
   * {@code mkdirs} is {@code true} and the directory cannot be created
   * @throws java.io.UncheckedIOException if creating the default temporary
   * directory fails
   */
  public File getValidStoreDir(@Nullable ApplicationTemp applicationTemp, boolean mkdirs) {
    return getValidStoreDir(applicationTemp, storeDir, mkdirs);
  }

  /**
   * Resolve a session data store directory, optionally creating it.
   * <p>If {@code dir} is {@code null}, obtain a directory from the given
   * {@link ApplicationTemp}, falling back to {@link ApplicationTemp#instance}.
   * The directory name is determined by the {@code infra.server.session.temp-dir}
   * strategy property and defaults to {@code server-sessions}. This temporary
   * directory is created as needed regardless of {@code mkdirs}.
   * <p>Otherwise, resolve a relative path against {@link ApplicationHome#getDir()
   * the application home directory} and create the directory and any missing
   * parent directories if {@code mkdirs} is {@code true}. An existing file is
   * rejected regardless of {@code mkdirs}; a nonexistent path is allowed when
   * {@code mkdirs} is {@code false}.
   *
   * @param applicationTemp the temporary directory provider used when {@code dir}
   * is {@code null}, or {@code null} to use {@link ApplicationTemp#instance}
   * @param dir the session data store directory, or {@code null} to use an
   * application-specific temporary directory
   * @param mkdirs whether to create a non-null {@code dir} if it does not exist
   * @return the resolved directory, which may not exist if {@code dir} is
   * non-null and {@code mkdirs} is {@code false}
   * @throws IllegalStateException if a non-null {@code dir} points to a file,
   * or {@code mkdirs} is {@code true} and the resolved directory does not exist
   * after an attempt to create it
   * @throws java.io.UncheckedIOException if creating the default temporary
   * directory fails
   */
  public static File getValidStoreDir(@Nullable ApplicationTemp applicationTemp, @Nullable File dir, boolean mkdirs) {
    if (dir == null) {
      return Objects.requireNonNullElse(applicationTemp, ApplicationTemp.instance).getDir(SESSION_TEMP_DIR).toFile();
    }
    if (!dir.isAbsolute()) {
      dir = new File(new ApplicationHome().getDir(), dir.getPath());
    }
    if (!dir.exists() && mkdirs) {
      dir.mkdirs();
    }
    assertDirectory(mkdirs, dir);
    return dir;
  }

  private static void assertDirectory(boolean mkdirs, File dir) {
    if (mkdirs && !dir.exists()) {
      throw new IllegalStateException("Session dir %s does not exist".formatted(dir));
    }

    if (dir.isFile()) {
      throw new IllegalStateException("Session dir %s points to a file".formatted(dir));
    }

  }

}
