/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.servlet.server;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import cn.taketoday.format.annotation.DurationUnit;

/**
 * Session properties.
 *
 * @author Andy Wilkinson
 * @since 4.0
 */
public class Session {

  @DurationUnit(ChronoUnit.SECONDS)
  private Duration timeout = Duration.ofMinutes(30);

  private Set<SessionTrackingMode> trackingModes;

  private boolean persistent;

  /**
   * Directory used to store session data.
   */
  private File storeDir;

  private final Cookie cookie = new Cookie();

  private final SessionStoreDirectory sessionStoreDirectory = new SessionStoreDirectory();

  public Duration getTimeout() {
    return this.timeout;
  }

  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }

  /**
   * Return the {@link SessionTrackingMode session tracking modes}.
   *
   * @return the session tracking modes
   */
  public Set<SessionTrackingMode> getTrackingModes() {
    return this.trackingModes;
  }

  public void setTrackingModes(Set<SessionTrackingMode> trackingModes) {
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
  public File getStoreDir() {
    return this.storeDir;
  }

  public void setStoreDir(File storeDir) {
    this.sessionStoreDirectory.setDirectory(storeDir);
    this.storeDir = storeDir;
  }

  public Cookie getCookie() {
    return this.cookie;
  }

  SessionStoreDirectory getSessionStoreDirectory() {
    return this.sessionStoreDirectory;
  }

  /**
   * Session cookie properties.
   */
  public static class Cookie extends cn.taketoday.framework.web.server.Cookie {

    /**
     * Comment for the session cookie.
     */
    private String comment;

    /**
     * Return the comment for the session cookie.
     *
     * @return the session cookie comment
     */
    public String getComment() {
      return this.comment;
    }

    public void setComment(String comment) {
      this.comment = comment;
    }

  }

  /**
   * Available session tracking modes (mirrors
   * {@link jakarta.servlet.SessionTrackingMode}.
   */
  public enum SessionTrackingMode {

    /**
     * Send a cookie in response to the client's first request.
     */
    COOKIE,

    /**
     * Rewrite the URL to append a session ID.
     */
    URL,

    /**
     * Use SSL build-in mechanism to track the session.
     */
    SSL

  }

}
