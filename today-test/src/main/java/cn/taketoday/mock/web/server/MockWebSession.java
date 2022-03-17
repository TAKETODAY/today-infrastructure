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

package cn.taketoday.mock.web.server;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.server.WebSession;
import cn.taketoday.web.server.session.InMemoryWebSessionStore;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@code WebSession} that delegates to a session instance
 * obtained via {@link InMemoryWebSessionStore}.
 *
 * <p>This is intended for use with the
 * {@link MockServerWebExchange.Builder#session(WebSession) session(WebSession)}
 * method of the {@code MockServerWebExchange} builder, eliminating the need
 * to use {@code WebSessionManager} or {@code WebSessionStore} altogether.
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public class MockWebSession implements WebSession {

  private final WebSession delegate;

  public MockWebSession() {
    this(null);
  }

  public MockWebSession(@Nullable Clock clock) {
    InMemoryWebSessionStore sessionStore = new InMemoryWebSessionStore();
    if (clock != null) {
      sessionStore.setClock(clock);
    }
    WebSession session = sessionStore.createWebSession().block();
    Assert.state(session != null, "WebSession must not be null");
    this.delegate = session;
  }

  @Override
  public String getId() {
    return this.delegate.getId();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return this.delegate.getAttributes();
  }

  @Override
  public void start() {
    this.delegate.start();
  }

  @Override
  public boolean isStarted() {
    return this.delegate.isStarted();
  }

  @Override
  public Mono<Void> changeSessionId() {
    return this.delegate.changeSessionId();
  }

  @Override
  public Mono<Void> invalidate() {
    return this.delegate.invalidate();
  }

  @Override
  public Mono<Void> save() {
    return this.delegate.save();
  }

  @Override
  public boolean isExpired() {
    return this.delegate.isExpired();
  }

  @Override
  public Instant getCreationTime() {
    return this.delegate.getCreationTime();
  }

  @Override
  public Instant getLastAccessTime() {
    return this.delegate.getLastAccessTime();
  }

  @Override
  public void setMaxIdleTime(Duration maxIdleTime) {
    this.delegate.setMaxIdleTime(maxIdleTime);
  }

  @Override
  public Duration getMaxIdleTime() {
    return this.delegate.getMaxIdleTime();
  }

}
