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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A message returned from the Docker API.
 *
 * @author Scott Frederick
 * @since 2.3.1
 */
public class Message {

  private final String message;

  @JsonCreator
  Message(@JsonProperty("message") String message) {
    this.message = message;
  }

  /**
   * Return the message contained in the response.
   *
   * @return the message
   */
  public String getMessage() {
    return this.message;
  }

  @Override
  public String toString() {
    return this.message;
  }

}
