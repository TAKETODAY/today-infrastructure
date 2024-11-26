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

package infra.test.web.client;

/**
 * An extension of {@code ResponseActions} that also implements
 * {@code RequestMatcher} and {@code ResponseCreator}
 *
 * <p>While {@code ResponseActions} is the API for defining expectations this
 * sub-interface is the internal SPI for matching these expectations to actual
 * requests and for creating responses.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface RequestExpectation extends ResponseActions, RequestMatcher, ResponseCreator {

  /**
   * Whether there is a remaining count of invocations for this expectation.
   */
  boolean hasRemainingCount();

  /**
   * Increase the matched request count and check we haven't passed the max count.
   *
   * @since 4.0
   */
  void incrementAndValidate();

  /**
   * Whether the requirements for this request expectation have been met.
   */
  boolean isSatisfied();

}
