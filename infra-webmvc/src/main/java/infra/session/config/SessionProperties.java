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
import infra.lang.Assert;
import infra.lang.TodayStrategies;

/**
 * Session properties.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties(prefix = "server.session", ignoreUnknownFields = true)
public class SessionProperties {

  private static final String SESSION_TEMP_DIR = TodayStrategies.getProperty(
          "server.session.temp-dir", "server-sessions");

  /**
   * session timeout
   */
  @Nullable
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration timeout = Duration.ofMinutes(30);

  /**
   * Whether to persist session data between restarts.
   */
  private boolean persistent;

  /**
   * Directory used to store session data.
   */
  @Nullable
  private File storeDir;

  /**
   * Session cookie config
   */
  @NestedConfigurationProperty
  public final CookieProperties cookie = new CookieProperties();

  /**
   * Session Id length
   */
  private int sessionIdLength = 32;

  /**
   * Set the maximum number of sessions that can be stored. Once the limit is
   * reached, any attempt to store an additional session will result in an
   * {@link IllegalStateException}.
   * <p>By default set to 10000.
   */
  private int maxSessions = 10000;

  /**
   * Set the maximum number of sessions that can be stored. Once the limit is
   * reached, any attempt to store an additional session will result in an
   * {@link IllegalStateException}.
   * <p>By default set to 10000.
   *
   * @param maxSessions the maximum number of sessions
   */
  public void setMaxSessions(int maxSessions) {
    this.maxSessions = maxSessions;
  }

  /**
   * Return the maximum number of sessions that can be stored.
   */
  public int getMaxSessions() {
    return this.maxSessions;
  }

  public void setSessionIdLength(int sessionIdLength) {
    Assert.isTrue(sessionIdLength > 0, "Session id length must > 0");
    this.sessionIdLength = sessionIdLength;
  }

  public int getSessionIdLength() {
    return sessionIdLength;
  }

  @Nullable
  public Duration getTimeout() {
    return this.timeout;
  }

  public void setTimeout(@Nullable Duration timeout) {
    this.timeout = timeout;
  }

  /**
   * Return whether to persist session data between restarts.
   *
   * @return {@code true} to persist session data between restarts.
   */
  public boolean isPersistent() {
    return this.persistent;
  }

  public void setPersistent(boolean persistent) {
    this.persistent = persistent;
  }

  /**
   * Return the directory used to store session data.
   *
   * @return the session data store directory
   */
  @Nullable
  public File getStoreDir() {
    return this.storeDir;
  }

  public void setStoreDir(@Nullable File storeDir) {
    this.storeDir = storeDir;
  }

  public File getValidStoreDir(@Nullable ApplicationTemp applicationTemp) {
    return getValidStoreDir(applicationTemp, true);
  }

  public File getValidStoreDir(@Nullable ApplicationTemp applicationTemp, boolean mkdirs) {
    return getValidStoreDir(applicationTemp, storeDir, mkdirs);
  }

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
