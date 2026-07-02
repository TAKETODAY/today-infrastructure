/*
 * Copyright 2002-present the original author or authors.
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

package infra.web.mock;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import infra.lang.Assert;
import infra.session.AbstractSession;
import infra.session.Session;
import infra.session.SessionEventDispatcher;

/**
 * Mock implementation of the {@link Session} interface.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Mark Fisher
 * @author Sam Brannen
 * @author Vedran Pavic
 * @since 4.0
 */
public class MockSession extends AbstractSession {

  /**
   * Default {@link #setMaxIdleTime(Duration)} (30 minutes).
   */
  public static final int DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS = 1800;

  private static int nextId = 1;

  private String id;

  private boolean invalid = false;

  private boolean isNew = true;

  private final Instant creationTime = Instant.now();

  private Instant lastAccessTime = this.creationTime;

  /**
   * Defaults to 30 minutes.
   */
  private Duration maxIdleTime = Duration.ofSeconds(DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS);

  /**
   * Create a new MockHttpSession with a default {@link DefaultMockContext}.
   *
   * @see DefaultMockContext
   */
  public MockSession() {
    this(null);
  }

  /**
   * Create a new MockHttpSession.
   *
   * @param id a unique identifier for this session
   */
  public MockSession(@Nullable String id) {
    super(new SessionEventDispatcher());
    this.id = (id != null ? id : Integer.toString(nextId++));
  }

  @Override
  public Instant getCreationTime() {
    assertIsValid();
    return creationTime;
  }

  @Override
  public Instant getLastAccessTime() {
    assertIsValid();
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
    assertIsValid();
    return maxIdleTime;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public String changeSessionId() {
    this.id = Integer.toString(nextId++);
    return id;
  }

  public void access() {
    lastAccessTime = Instant.now();
    this.isNew = false;
  }

  @Override
  public @Nullable Object getAttribute(String name) {
    assertIsValid();
    Assert.notNull(name, "Attribute name is required");
    return super.getAttribute(name);
  }

  @Override
  public Iterable<String> attributeNames() {
    assertIsValid();
    return super.attributeNames();
  }

  @Override
  public String[] getAttributeNames() {
    assertIsValid();
    return super.getAttributeNames();
  }

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    assertIsValid();
    super.setAttribute(name, value);
  }

  @Override
  public void setAttributes(@Nullable Map<String, Object> attributes) {
    assertIsValid();
    super.setAttributes(attributes);
  }

  @Override
  public @Nullable Object removeAttribute(String name) {
    assertIsValid();
    Assert.notNull(name, "Attribute name is required");
    return super.removeAttribute(name);
  }

  /**
   * Invalidates this session then unbinds any objects bound to it.
   *
   * @throws IllegalStateException if this method is called on an already invalidated session
   */
  @Override
  public void invalidate() {
    assertIsValid();
    super.invalidate();
    this.invalid = true;
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

  public boolean isInvalid() {
    return this.invalid;
  }

  /**
   * Convenience method for asserting that this session has not been
   * {@linkplain #invalidate() invalidated}.
   *
   * @throws IllegalStateException if this session has been invalidated
   */
  private void assertIsValid() {
    Assert.state(!isInvalid(), "The session has already been invalidated");
  }

  public void setNew(boolean value) {
    this.isNew = value;
  }

  @Override
  public boolean isNew() {
    assertIsValid();
    return this.isNew;
  }

  public SessionEventDispatcher events() {
    return eventDispatcher;
  }

}
