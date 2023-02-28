/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.session.config;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.format.annotation.DurationUnit;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Session properties.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties(prefix = "server.session", ignoreUnknownFields = true)
public class SessionProperties {

  @Nullable
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration timeout = Duration.ofMinutes(30);

  @Nullable
  private Set<SessionTrackingMode> trackingModes;

  private boolean persistent;

  /**
   * Directory used to store session data.
   */
  @Nullable
  private File storeDir;

  private final CookieProperties cookie = new CookieProperties();

  private int sessionIdLength = 30;

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
   * Return the {@link SessionTrackingMode session tracking modes}.
   *
   * @return the session tracking modes
   */
  @Nullable
  public Set<SessionTrackingMode> getTrackingModes() {
    return this.trackingModes;
  }

  public void setTrackingModes(@Nullable Set<SessionTrackingMode> trackingModes) {
    this.trackingModes = trackingModes;
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

  public CookieProperties getCookie() {
    return this.cookie;
  }

}
