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

package cn.taketoday.buildpack.platform.docker.transport;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * Exception thrown when connection to the Docker daemon fails.
 *
 * @author Scott Frederick
 * @since 4.0
 */
public class DockerConnectionException extends RuntimeException {

  private static final String JNA_EXCEPTION_CLASS_NAME = "com.sun.jna.LastErrorException";

  public DockerConnectionException(String host, Exception cause) {
    super(buildMessage(host, cause), cause);
  }

  private static String buildMessage(String host, Exception cause) {
    Assert.notNull(host, "Host must not be null");
    Assert.notNull(cause, "Cause must not be null");
    StringBuilder message = new StringBuilder("Connection to the Docker daemon at '" + host + "' failed");
    String causeMessage = getCauseMessage(cause);
    if (StringUtils.hasText(causeMessage)) {
      message.append(" with error \"").append(causeMessage).append("\"");
    }
    message.append("; ensure the Docker daemon is running and accessible");
    return message.toString();
  }

  private static String getCauseMessage(Exception cause) {
    if (cause.getCause() != null && cause.getCause().getClass().getName().equals(JNA_EXCEPTION_CLASS_NAME)) {
      return cause.getCause().getMessage();
    }
    return cause.getMessage();
  }

}
