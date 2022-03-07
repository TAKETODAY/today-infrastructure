/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.beans.factory.parsing;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Represents a problem with a bean definition configuration.
 * Mainly serves as common argument passed into a {@link ProblemReporter}.
 *
 * <p>May indicate a potentially fatal problem (an error) or just a warning.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see ProblemReporter
 * @since 4.0
 */
public class Problem {

  private final String message;

  private final Location location;

  @Nullable
  private final ParseState parseState;

  @Nullable
  private final Throwable rootCause;

  /**
   * Create a new instance of the {@link Problem} class.
   *
   * @param message a message detailing the problem
   * @param location the location within a bean configuration source that triggered the error
   */
  public Problem(String message, Location location) {
    this(message, location, null, null);
  }

  /**
   * Create a new instance of the {@link Problem} class.
   *
   * @param message a message detailing the problem
   * @param parseState the {@link ParseState} at the time of the error
   * @param location the location within a bean configuration source that triggered the error
   */
  public Problem(String message, Location location, ParseState parseState) {
    this(message, location, parseState, null);
  }

  /**
   * Create a new instance of the {@link Problem} class.
   *
   * @param message a message detailing the problem
   * @param rootCause the underlying exception that caused the error (may be {@code null})
   * @param parseState the {@link ParseState} at the time of the error
   * @param location the location within a bean configuration source that triggered the error
   */
  public Problem(String message, Location location, @Nullable ParseState parseState, @Nullable Throwable rootCause) {
    Assert.notNull(message, "Message must not be null");
    Assert.notNull(location, "Location must not be null");
    this.message = message;
    this.location = location;
    this.parseState = parseState;
    this.rootCause = rootCause;
  }

  /**
   * Get the message detailing the problem.
   */
  public String getMessage() {
    return this.message;
  }

  /**
   * Get the location within a bean configuration source that triggered the error.
   */
  public Location getLocation() {
    return this.location;
  }

  /**
   * Get the description of the bean configuration source that triggered the error,
   * as contained within this Problem's Location object.
   *
   * @see #getLocation()
   */
  public String getResourceDescription() {
    return getLocation().getResource().getDescription();
  }

  /**
   * Get the {@link ParseState} at the time of the error (may be {@code null}).
   */
  @Nullable
  public ParseState getParseState() {
    return this.parseState;
  }

  /**
   * Get the underlying exception that caused the error (may be {@code null}).
   */
  @Nullable
  public Throwable getRootCause() {
    return this.rootCause;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Configuration problem: ");
    sb.append(getMessage());
    sb.append("\nOffending resource: ").append(getResourceDescription());
    if (getParseState() != null) {
      sb.append('\n').append(getParseState());
    }
    return sb.toString();
  }

}
