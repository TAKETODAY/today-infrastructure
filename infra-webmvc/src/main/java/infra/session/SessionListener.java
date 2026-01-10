/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.session;

import java.util.EventListener;

/**
 * Interface for receiving notification events about Session lifecycle changes.
 *
 * <p>
 * In order to receive these notification events, the implementation
 * class must be either declared in the deployment
 * descriptor of the web application
 *
 * <p>
 * Implementations of this interface are invoked at their {@link #sessionCreated}
 * method in the order in which they have been declared, and at their {@link #sessionDestroyed}
 * method in reverse order.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Session
 * @since 4.0 2022/4/9 09:56
 */
public interface SessionListener extends EventListener {

  /**
   * Receives notification that a session has been created.
   *
   * <p>
   * The default implementation takes no action.
   *
   * @param session the session
   */
  default void sessionCreated(Session session) {
    // default do nothing
  }

  /**
   * Receives notification that a session is about to be invalidated.
   * <p>
   * The default implementation takes no action.
   *
   * @param session the session
   */
  default void sessionDestroyed(Session session) {
    // default do nothing
  }

}
