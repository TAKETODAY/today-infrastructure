/*
 * Copyright 2025-present the original author or authors.
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

package infra.shell.command;

/**
 * Exception to signal that an error happened while executing a command.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public class CommandExecutionException extends RuntimeException {

  private final int exitCode;

  /**
   * Create a new {@code CommandExecutionException} with the given message.
   *
   * @param message the detail message
   */
  public CommandExecutionException(String message) {
    this(message, ExitStatus.EXECUTION_ERROR.code());
  }

  /**
   * Create a new {@code CommandExecutionException} with the given message and exit
   * code.
   *
   * @param message the detail message
   * @param exitCode the exit code associated with this exception
   * @since 5.0
   */
  public CommandExecutionException(String message, int exitCode) {
    super(message);
    this.exitCode = exitCode;
  }

  /**
   * Create a new {@code CommandExecutionException} with the given message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public CommandExecutionException(String message, Throwable cause) {
    super(message, cause);
    this.exitCode = ExitStatus.EXECUTION_ERROR.code();
  }

  /**
   * Return the exit code associated with this exception.
   *
   * @return the exit code
   * @since 5.0
   */
  public int getExitCode() {
    return this.exitCode;
  }

}
