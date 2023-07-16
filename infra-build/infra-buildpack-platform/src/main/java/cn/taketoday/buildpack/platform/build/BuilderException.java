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

package cn.taketoday.buildpack.platform.build;

/**
 * Exception thrown to indicate a Builder error.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BuilderException extends RuntimeException {

  private final String operation;

  private final int statusCode;

  BuilderException(String operation, int statusCode) {
    super(buildMessage(operation, statusCode));
    this.operation = operation;
    this.statusCode = statusCode;
  }

  /**
   * Return the Builder operation that failed.
   *
   * @return the operation description
   */
  public String getOperation() {
    return this.operation;
  }

  /**
   * Return the status code returned from a Builder operation.
   *
   * @return the statusCode the status code
   */
  public int getStatusCode() {
    return this.statusCode;
  }

  private static String buildMessage(String operation, int statusCode) {
    StringBuilder message = new StringBuilder("Builder");
    if (operation != null && !operation.isEmpty()) {
      message.append(" lifecycle '").append(operation).append("'");
    }
    message.append(" failed with status code ").append(statusCode);
    return message.toString();
  }

}
