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

package cn.taketoday.session;

import java.io.Serial;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.lang.Assert;

/**
 * <p>
 * A {@link WebSession} implementation that is backed by a {@link java.util.Map}. The
 * defaults for the properties are:
 * </p>
 * <ul>
 * <li>id - a secure random generated id</li>
 * <li>creationTime - the moment the {@link MapSession} was instantiated</li>
 * <li>lastAccessTime - the moment the {@link MapSession} was instantiated</li>
 * <li>maxIdleTime - 30 minutes</li>
 * </ul>
 *
 * <p>
 * This implementation has no synchronization, so it is best to use the copy constructor
 * when working on multiple threads.
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/10 15:52
 */
public class MapSession extends AttributeAccessorSupport implements WebSession {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Default {@link #setMaxIdleTime(Duration)} (30 minutes).
   */
  public static final int DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS = 1800;

  private String id;

  private final String originalId;

  private Instant creationTime = Instant.now();

  private Instant lastAccessTime = this.creationTime;

  /**
   * Defaults to 30 minutes.
   */
  private Duration maxIdleTime = Duration.ofSeconds(DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS);

  /**
   * Creates a new instance with a secure randomly generated identifier.
   */
  public MapSession() {
    this(UUID.randomUUID().toString());
  }

  /**
   * Creates a new instance with the specified id. This is preferred to the default
   * constructor when the id is known to prevent unnecessary consumption on entropy
   * which can be slow.
   *
   * @param id the identifier to use
   */
  public MapSession(String id) {
    this.id = id;
    this.originalId = id;
  }

  /**
   * Creates a new instance from the provided {@link WebSession}.
   *
   * @param session the {@link WebSession} to initialize this {@link WebSession} with. Cannot
   * be null.
   */
  public MapSession(WebSession session) {
    Assert.notNull(session, "session cannot be null");
    this.id = session.getId();
    this.originalId = this.id;
    this.maxIdleTime = session.getMaxIdleTime();
    this.creationTime = session.getCreationTime();
    this.lastAccessTime = session.getLastAccessTime();

    copyAttributesFrom(session);
  }

  @Override
  public String getId() {
    return id;
  }

  /**
   * Get the original session id.
   *
   * @return the original session id
   * @see #changeSessionId()
   */
  public String getOriginalId() {
    return this.originalId;
  }

  @Override
  public void start() {

  }

  @Override
  public boolean isStarted() {
    return true;
  }

  @Override
  public void changeSessionId() {
    this.id = generateId();
  }

  @Override
  public void save() {

  }

  @Override
  public void invalidate() {
    clearAttributes();
  }

  @Override
  public boolean isExpired() {
    return isExpired(Instant.now());
  }

  boolean isExpired(Instant now) {
    if (maxIdleTime.isNegative()) {
      return false;
    }
    return now.minus(maxIdleTime).compareTo(lastAccessTime) >= 0;
  }

  @Override
  public Instant getCreationTime() {
    return creationTime;
  }

  @Override
  public Instant getLastAccessTime() {
    return lastAccessTime;
  }

  @Override
  public void setLastAccessTime(Instant lastAccessTime) {
    this.lastAccessTime = lastAccessTime;
  }

  @Override
  public void setMaxIdleTime(Duration maxIdleTime) {
    this.maxIdleTime = maxIdleTime;
  }

  @Override
  public Duration getMaxIdleTime() {
    return maxIdleTime;
  }

  /**
   * Sets the time that this {@link WebSession} was created. The default is when the
   * {@link WebSession} was instantiated.
   *
   * @param creationTime the time that this {@link WebSession} was created.
   */
  public void setCreationTime(Instant creationTime) {
    this.creationTime = creationTime;
  }

  /**
   * Sets the identifier for this {@link WebSession}. The id should be a secure random
   * generated value to prevent malicious users from guessing this value. The default is
   * a secure random generated identifier.
   *
   * @param id the identifier for this session.
   */
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj instanceof WebSession session && id.equals(session.getId()));
  }

  @Override
  public int hashCode() {
    return this.id.hashCode();
  }

  private static String generateId() {
    return UUID.randomUUID().toString();
  }
}
