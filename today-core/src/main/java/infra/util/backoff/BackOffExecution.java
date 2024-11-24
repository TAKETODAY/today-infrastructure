/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */
package infra.util.backoff;

/**
 * Represent a particular back-off execution.
 *
 * <p>Implementations do not need to be thread safe.
 *
 * @author Stephane Nicoll
 * @see BackOff
 * @since 4.0
 */
@FunctionalInterface
public interface BackOffExecution {

  /**
   * Return value of {@link #nextBackOff()} that indicates that the operation
   * should not be retried.
   */
  long STOP = -1;

  /**
   * Return the number of milliseconds to wait before retrying the operation
   * or {@link #STOP} ({@value #STOP}) to indicate that no further attempt
   * should be made for the operation.
   */
  long nextBackOff();

}
