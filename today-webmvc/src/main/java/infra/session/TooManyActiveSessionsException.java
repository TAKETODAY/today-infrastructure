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

import java.io.Serial;

/**
 * An exception that indicates the maximum number of active sessions has been
 * reached and the server is refusing to create any new sessions.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/28 22:35
 */
public class TooManyActiveSessionsException extends IllegalStateException {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * The maximum number of active sessions the server will tolerate.
   */
  private final int maxActiveSessions;

  /**
   * Creates a new TooManyActiveSessionsException.
   *
   * @param message A description for the exception.
   * @param maxActive The maximum number of active sessions allowed by the
   * session manager.
   */
  public TooManyActiveSessionsException(String message, int maxActive) {
    super(message);
    this.maxActiveSessions = maxActive;
  }

  /**
   * Gets the maximum number of sessions allowed by the session manager.
   *
   * @return The maximum number of sessions allowed by the session manager.
   */
  public int getMaxActiveSessions() {
    return maxActiveSessions;
  }

}

