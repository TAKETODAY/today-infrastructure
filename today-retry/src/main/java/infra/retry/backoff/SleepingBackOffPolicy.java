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

package infra.retry.backoff;

/**
 * A interface which can be mixed in by {@link BackOffPolicy}s indicating that they sleep
 * when backing off.
 *
 * @param <T> the type of the policy itself
 * @since 4.0
 */
public interface SleepingBackOffPolicy<T extends SleepingBackOffPolicy<T>> extends BackOffPolicy {

  /**
   * Clone the policy and return a new policy which uses the passed sleeper.
   *
   * @param sleeper Target to be invoked any time the backoff policy sleeps
   * @return a clone of this policy which will have all of its backoff sleeps routed
   * into the passed sleeper
   */
  T withSleeper(Sleeper sleeper);

}
