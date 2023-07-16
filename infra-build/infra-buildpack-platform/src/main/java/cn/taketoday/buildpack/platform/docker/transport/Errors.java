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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Errors returned from the Docker API.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class Errors implements Iterable<Errors.Error> {

  private final List<Error> errors;

  @JsonCreator
  Errors(@JsonProperty("errors") List<Error> errors) {
    this.errors = (errors != null) ? errors : Collections.emptyList();
  }

  @Override
  public Iterator<Errors.Error> iterator() {
    return this.errors.iterator();
  }

  /**
   * Returns a sequential {@code Stream} of the errors.
   *
   * @return a stream of the errors
   */
  public Stream<Error> stream() {
    return this.errors.stream();
  }

  /**
   * Return if there are any contained errors.
   *
   * @return if the errors are empty
   */
  public boolean isEmpty() {
    return this.errors.isEmpty();
  }

  @Override
  public String toString() {
    return this.errors.toString();
  }

  /**
   * An individual Docker error.
   */
  public static class Error {

    private final String code;

    private final String message;

    @JsonCreator
    Error(String code, String message) {
      this.code = code;
      this.message = message;
    }

    /**
     * Return the error code.
     *
     * @return the error code
     */
    public String getCode() {
      return this.code;
    }

    /**
     * Return the error message.
     *
     * @return the error message
     */
    public String getMessage() {
      return this.message;
    }

    @Override
    public String toString() {
      return this.code + ": " + this.message;
    }

  }

}
