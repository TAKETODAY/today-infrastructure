/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.socket;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;

/**
 * @author TODAY 2021/5/7 23:03
 * @since 3.0.1
 */
public abstract class NativeWebSocketSession<T> extends WebSocketSession {
  protected T nativeSession;

  public NativeWebSocketSession(HttpHeaders handshakeHeaders) {
    super(handshakeHeaders);
  }

  protected final void checkNativeSessionInitialized() {
    Assert.state(this.nativeSession != null, "WebSocket session is not yet initialized");
  }

  public void initializeNativeSession(T nativeSession) {
    this.nativeSession = nativeSession;
  }

  public final T obtainNativeSession() {
    final T session = this.nativeSession;
    Assert.state(session != null, "No native session");
    return session;
  }

}
