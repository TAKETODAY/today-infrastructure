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

package infra.web.context.support;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

import infra.beans.factory.config.Scope;
import infra.session.Session;
import infra.session.SessionAttributeListener;
import infra.session.SessionManager;
import infra.web.HttpExchangeHolder;
import infra.web.ServerHttpExchange;
import infra.web.util.WebUtils;

/**
 * Session-backed {@link Scope} implementation.
 *
 * <p>Relies on a thread-bound {@link ServerHttpExchange} instance
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SessionManager
 * @since 4.0 2022/2/21 11:40
 */
public class SessionScope extends AbstractWebScope {

  /**
   * Constant identifying the {@link String} prefixed to the name of a
   * destruction callback when it is stored in a {@link Session}.
   */
  public static final String DESTRUCTION_CALLBACK_NAME_PREFIX =
          SessionScope.class.getName() + ".DESTRUCTION_CALLBACK.";

  @Override
  public @Nullable String getConversationId() {
    return HttpExchangeHolder.required().getSessionId();
  }

  @Override
  public Object get(String name, Supplier<?> objectFactory) {
    Session session = HttpExchangeHolder.required().getSession();
    Object sessionMutex = WebUtils.getSessionMutex(session);
    synchronized(sessionMutex) {
      return doGetBean(session, name, objectFactory);
    }
  }

  @Override
  public @Nullable Object remove(String name) {
    ServerHttpExchange exchange = HttpExchangeHolder.current();
    if (exchange != null) {
      Session session = exchange.getSession(false);
      if (session != null) {
        Object sessionMutex = WebUtils.getSessionMutex(session);
        synchronized(sessionMutex) {
          return remove(session, name);
        }
      }
    }
    return null;
  }

  @Override
  public @Nullable Object resolveContextualObject(String key) {
    if (ServerHttpExchange.SCOPE_REQUEST.equals(key)) {
      return HttpExchangeHolder.current();
    }
    else if (ServerHttpExchange.SCOPE_SESSION.equals(key)) {
      ServerHttpExchange exchange = HttpExchangeHolder.current();
      if (exchange != null) {
        return exchange.getSession(false);
      }
    }
    return null;
  }

  @Override
  public void registerDestructionCallback(String name, Runnable callback) {
    HttpExchangeHolder.required().setSessionAttribute(getDestructionCallbackName(name),
            new DestructionCallbackBindingListener(callback));
  }

  static String getDestructionCallbackName(String name) {
    return DESTRUCTION_CALLBACK_NAME_PREFIX + name;
  }

  public static SessionAttributeListener createDestructionCallback() {
    return new DestructionCallback();
  }

  static final class DestructionCallback implements SessionAttributeListener {

    @Override
    public void attributeRemoved(Session session, String attributeName, @Nullable Object value) {
      // notify DestructionCallbackBindingListener
      String destructionName = getDestructionCallbackName(attributeName);
      if (!Objects.equals(destructionName, attributeName)) {
        session.removeAttribute(destructionName);
      }
    }
  }
}
