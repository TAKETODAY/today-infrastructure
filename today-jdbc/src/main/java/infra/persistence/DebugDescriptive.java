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
 * An interface that extends {@code Descriptive} and provides additional
 * functionality for generating debug log messages. This interface is typically
 * used in scenarios where descriptive information about an object needs to be
 * logged or displayed for debugging purposes.
 *
 * <p>The {@link #getDescription()} method provides a human-readable description
 * of the object, while the {@link #getDebugLogMessage()} method generates a
 * formatted log message suitable for debugging.
 *
 * <p>This interface is particularly useful in frameworks or libraries where
 * debugging information needs to be dynamically generated based on the state
 * of objects at runtime.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/31 16:47
 */
public interface DebugDescriptive extends Descriptive {

  /**
   * Returns a human-readable description of the object.
   * This method is intended to provide a concise and meaningful
   * summary of the object's state or purpose, typically used for
   * debugging or logging purposes.
   *
   * <p>The description should be brief but informative enough to
   * help developers understand the context or state of the object
   * when reviewing logs or debugging information.
   *
   * <p><b>Example Usage:</b>
   * <pre>{@code
   * DebugDescriptive debugObject = new DebugDescriptive() {
   *   @Override
   *   public String getDescription() {
   *     return "This is a sample description.";
   *   }
   * };
   *
   * System.out.println(debugObject.getDescription());
   * }</pre>
   *
   * In this example, the output will be:
   * <pre>{@code
   * This is a sample description.
   * }</pre>
   *
   * @return a {@link String} representing the human-readable description
   * of the object
   */
  @Override
  String getDescription();

  /**
   * Returns a debug log message based on the description of the object.
   * The message is formatted using {@link LogMessage#format(String, Object...)}
   * with the result of {@link #getDescription()} as the content.
   *
   * <p>This method is particularly useful for generating log messages that
   * provide detailed information about the state or purpose of an object
   * during debugging. The returned object is an instance of {@link LogMessage},
   * which supports lazy evaluation of the message string.
   *
   * <p><b>Example Usage:</b>
   * <pre>{@code
   * DebugDescriptive debugObject = new DebugDescriptive() {
   *   @Override
   *   public String getDescription() {
   *     return "This is a sample description.";
   *   }
   * };
   *
   * Object debugLogMessage = debugObject.getDebugLogMessage();
   * System.out.println(debugLogMessage.toString());
   * }</pre>
   *
   * In this example, the output will be a formatted log message containing
   * the description "This is a sample description.".
   *
   * @return a {@link LogMessage} object containing the formatted debug log message
   */
  default Object getDebugLogMessage() {
    return LogMessage.format(getDescription());
  }

}
