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

import java.net.URI;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * Exception thrown when a call to the Docker API fails.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 4.0
 */
public class DockerEngineException extends RuntimeException {

  private final int statusCode;

  private final String reasonPhrase;

  private final Errors errors;

  private final Message responseMessage;

  public DockerEngineException(String host, URI uri, int statusCode, String reasonPhrase, Errors errors,
          Message responseMessage) {
    super(buildMessage(host, uri, statusCode, reasonPhrase, errors, responseMessage));
    this.statusCode = statusCode;
    this.reasonPhrase = reasonPhrase;
    this.errors = errors;
    this.responseMessage = responseMessage;
  }

  /**
   * Return the status code returned by the Docker API.
   *
   * @return the statusCode the status code
   */
  public int getStatusCode() {
    return this.statusCode;
  }

  /**
   * Return the reason phrase returned by the Docker API.
   *
   * @return the reasonPhrase
   */
  public String getReasonPhrase() {
    return this.reasonPhrase;
  }

  /**
   * Return the errors from the body of the Docker API response, or {@code null} if the
   * errors JSON could not be read.
   *
   * @return the errors or {@code null}
   */
  public Errors getErrors() {
    return this.errors;
  }

  /**
   * Return the message from the body of the Docker API response, or {@code null} if the
   * message JSON could not be read.
   *
   * @return the message or {@code null}
   */
  public Message getResponseMessage() {
    return this.responseMessage;
  }

  private static String buildMessage(String host, URI uri, int statusCode, String reasonPhrase, Errors errors,
          Message responseMessage) {
    Assert.notNull(host, "Host is required");
    Assert.notNull(uri, "URI is required");
    StringBuilder message = new StringBuilder(
            "Docker API call to '" + host + uri + "' failed with status code " + statusCode);
    if (StringUtils.isNotEmpty(reasonPhrase)) {
      message.append(" \"").append(reasonPhrase).append("\"");
    }
    if (responseMessage != null && StringUtils.isNotEmpty(responseMessage.getMessage())) {
      message.append(" and message \"").append(responseMessage.getMessage()).append("\"");
    }
    if (errors != null && !errors.isEmpty()) {
      message.append(" ").append(errors);
    }
    return message.toString();
  }

}
