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

package cn.taketoday.buildpack.platform.docker.type;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;

import com.fasterxml.jackson.databind.JsonNode;

import cn.taketoday.buildpack.platform.json.MappedObject;

/**
 * Status details returned from {@code Docker container wait}.
 *
 * @author Scott Frederick
 * @since 4.0
 */
public class ContainerStatus extends MappedObject {

  private final int statusCode;

  private final String waitingErrorMessage;

  ContainerStatus(int statusCode, String waitingErrorMessage) {
    super(null, null);
    this.statusCode = statusCode;
    this.waitingErrorMessage = waitingErrorMessage;
  }

  ContainerStatus(JsonNode node) {
    super(node, MethodHandles.lookup());
    this.statusCode = valueAt("/StatusCode", Integer.class);
    this.waitingErrorMessage = valueAt("/Error/Message", String.class);
  }

  /**
   * Return the container exit status code.
   *
   * @return the exit status code
   */
  public int getStatusCode() {
    return this.statusCode;
  }

  /**
   * Return a message indicating an error waiting for a container to stop.
   *
   * @return the waiting error message
   */
  public String getWaitingErrorMessage() {
    return this.waitingErrorMessage;
  }

  /**
   * Create a new {@link ContainerStatus} instance from the specified JSON content
   * stream.
   *
   * @param content the JSON content stream
   * @return a new {@link ContainerStatus} instance
   * @throws IOException on IO error
   */
  public static ContainerStatus of(InputStream content) throws IOException {
    return of(content, ContainerStatus::new);
  }

  /**
   * Create a new {@link ContainerStatus} instance with the specified values.
   *
   * @param statusCode the status code
   * @param errorMessage the error message
   * @return a new {@link ContainerStatus} instance
   */
  public static ContainerStatus of(int statusCode, String errorMessage) {
    return new ContainerStatus(statusCode, errorMessage);
  }

}
