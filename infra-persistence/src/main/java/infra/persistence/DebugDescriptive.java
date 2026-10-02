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

package infra.persistence;

import infra.lang.Descriptive;
import infra.logging.LogMessage;

/**
 * Provides a concise operation description and an optional detailed message
 * for debug logging.
 *
 * <p>{@link #getDescription()} identifies the operation for exception context,
 * while {@link #getDebugLogMessage()} may additionally include runtime details,
 * such as query parameters. Implementations may return a {@link LogMessage}
 * to defer formatting until the message is converted to text.
 *
 * <p>Producing either description should not perform database access or modify
 * the state of the described object.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/31 16:47
 */
public interface DebugDescriptive extends Descriptive {

  /**
   * Return a concise, human-readable operation description suitable for
   * exception context, for example {@code "Query entities by ID"}.
   *
   * @return the non-null operation description
   */
  @Override
  String getDescription();

  /**
   * Return a message object whose {@code toString()} provides the debug text.
   * <p>The default implementation returns {@link #getDescription()} directly.
   * Overrides may include runtime details and use
   * {@link LogMessage#format(String, Object...)} for lazy formatting, for example
   * {@code LogMessage.format("Query entities by ID: {}", id)}.
   *
   * @return the non-null debug message, such as a String or LogMessage
   */
  default Object getDebugLogMessage() {
    return getDescription();
  }

}
