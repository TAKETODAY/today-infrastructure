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

package cn.taketoday.web.session;

import java.time.Duration;

import cn.taketoday.core.io.Resource;

/**
 * @author TODAY 2021/4/27 23:03
 * @since 3.0
 */
public class SessionProperties {

  /**
   * enable {@link jakarta.servlet.http.HttpSession}?
   */
  private boolean enableHttpSession = false;
  private boolean persistent = true;

  /** Directory used to store session data. */
  private Resource storeDirectory;
  private TrackingMode[] trackingModes;
  private Duration timeout = Duration.ofMinutes(30);

  private SessionCookieConfig cookieConfig;

  private int sessionIdLength = 30;

  public SessionProperties() { }

  public SessionProperties(SessionCookieConfig cookieConfig) {
    this.cookieConfig = cookieConfig;
  }

  public void setSessionIdLength(int sessionIdLength) {
    this.sessionIdLength = sessionIdLength;
  }

  public int getSessionIdLength() {
    return sessionIdLength;
  }

  public boolean isEnableHttpSession() {
    return enableHttpSession;
  }

  public boolean isPersistent() {
    return persistent;
  }

  public void setCookieConfig(SessionCookieConfig cookieConfig) {
    this.cookieConfig = cookieConfig;
  }

  public void setEnableHttpSession(boolean enableHttpSession) {
    this.enableHttpSession = enableHttpSession;
  }

  public void setPersistent(boolean persistent) {
    this.persistent = persistent;
  }

  public void setStoreDirectory(Resource storeDirectory) {
    this.storeDirectory = storeDirectory;
  }

  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }

  public void setTrackingModes(TrackingMode[] trackingModes) {
    this.trackingModes = trackingModes;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public Resource getStoreDirectory() {
    return storeDirectory;
  }

  public SessionCookieConfig getCookieConfig() {
    return cookieConfig;
  }

  public TrackingMode[] getTrackingModes() {
    return trackingModes;
  }
}
